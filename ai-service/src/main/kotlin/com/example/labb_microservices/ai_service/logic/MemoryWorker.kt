package com.example.labb_microservices.ai_service.logic

import com.example.labb_microservices.ai_service.model.AuthorType
import com.example.labb_microservices.ai_service.model.MemoryFragment
import com.example.labb_microservices.ai_service.model.Message
import com.example.labb_microservices.ai_service.repository.MemoryFragmentRepository
import com.example.labb_microservices.ai_service.model.PersonaUpdateEvent
import com.example.labb_microservices.ai_service.messaging.RabbitMQConfig
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.time.Instant
import kotlin.math.max

import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update

@Service
class MemoryWorker(
    private val factExtractor: FactExtractor,
    private val memoryFragmentRepository: MemoryFragmentRepository,
    private val mongoTemplate: ReactiveMongoOperations,
    private val rabbitTemplate: RabbitTemplate
) {
    private val logger = LoggerFactory.getLogger(MemoryWorker::class.java)

    fun processMessageForMemory(message: Message): Mono<Void> {
        if (message.authorType == AuthorType.BOT) return Mono.empty()

        return factExtractor.extractFacts(message)
            .flatMap { fact ->
                val query = Query(Criteria.where("userId").`is`(message.senderId)
                    .and("category").`is`(fact.category)
                    .and("value").`is`(fact.value))
                
                val update = Update()
                    .set("confidence", fact.confidence)
                    .set("timestamp", Instant.now())
                    .set("sourceMessageId", message.id ?: "unknown")
                    .setOnInsert("userId", message.senderId)
                    .setOnInsert("category", fact.category)
                    .setOnInsert("value", fact.value)

                mongoTemplate.upsert(query, update, MemoryFragment::class.java)
                    .flatMap { result ->
                        // Reload to check confidence and publish if needed
                        memoryFragmentRepository.findByUserIdAndCategoryAndValue(message.senderId, fact.category, fact.value)
                            .flatMap { saved ->
                                if (saved.confidence > 0.9) {
                                    publishPersonaUpdate(saved)
                                } else {
                                    Mono.empty()
                                }
                            }
                    }
            }
            .then()
    }

    private fun publishPersonaUpdate(fragment: MemoryFragment): Mono<Void> {
        return Mono.fromRunnable<Void> {
            logger.info("Publishing high-confidence persona update for user (ID redacted): category={}", fragment.category)
            val event = PersonaUpdateEvent(
                userId = fragment.userId,
                category = fragment.category,
                value = fragment.value
            )
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.PERSONA_EXCHANGE_NAME,
                "persona.update",
                event
            )
        }.subscribeOn(Schedulers.boundedElastic())
    }
}
