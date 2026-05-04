package com.example.labb_microservices.user_service.service

import com.example.labb_microservices.user_service.config.RabbitConfig
import com.example.labb_microservices.user_service.model.PresenceStatus
import com.example.labb_microservices.user_service.model.PresenceUpdateEvent
import com.example.labb_microservices.user_service.repository.PresenceTracker
import com.example.labb_microservices.user_service.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

import reactor.core.scheduler.Schedulers

@Service
class PresenceService(
    private val presenceTracker: PresenceTracker,
    private val userRepository: UserRepository,
    private val rabbitTemplate: RabbitTemplate
) {
    private val logger = LoggerFactory.getLogger(PresenceService::class.java)

    fun updateStatus(userId: String, status: PresenceStatus): Mono<Void> {
        return userRepository.findById(userId)
            .switchIfEmpty(Mono.error(RuntimeException("User not found")))
            .flatMap { user ->
                val username = user.username ?: "unknown"
                presenceTracker.setStatus(userId, status)
                    .flatMap {
                        Mono.fromCallable {
                            val event = PresenceUpdateEvent(
                                userId = userId,
                                username = username,
                                status = status
                            )
                            rabbitTemplate.convertAndSend(RabbitConfig.PRESENCE_EXCHANGE, "", event)
                            logger.info("Published presence update for user $userId: $status")
                        }
                        .subscribeOn(Schedulers.boundedElastic())
                        .onErrorResume { e ->
                            logger.error("Failed to publish presence update to RabbitMQ for user $userId", e)
                            Mono.empty()
                        }
                        .then()
                    }
            }
    }

    fun getStatus(userId: String): Mono<PresenceStatus> {
        return presenceTracker.getStatus(userId)
    }

    fun getAllPresences(): Flux<PresenceUpdateEvent> {
        return presenceTracker.getAllPresences()
            .flatMap { (userId, status) ->
                userRepository.findById(userId)
                    .map { user ->
                        PresenceUpdateEvent(userId, user.username ?: "unknown", status)
                    }
                    .defaultIfEmpty(PresenceUpdateEvent(userId, "unknown", status))
            }
    }
}
