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
import java.time.Instant
import kotlin.math.max

@Service
class MemoryWorker(
    private val factExtractor: FactExtractor,
    private val memoryFragmentRepository: MemoryFragmentRepository,
    private val rabbitTemplate: RabbitTemplate
) {
    private val logger = LoggerFactory.getLogger(MemoryWorker::class.java)

    fun processMessageForMemory(message: Message): Mono<Void> {
        if (message.authorType == AuthorType.BOT) return Mono.empty()

        return factExtractor.extractFacts(message)
            .flatMap { fact ->
                // Deduplicate/Update existing facts for the same user, category, and value
                memoryFragmentRepository.findByUserIdAndCategoryAndValue(message.senderId, fact.category, fact.value)
                    .flatMap { existing ->
                        val updated = existing.copy(
                            confidence = max(existing.confidence, fact.confidence),
                            timestamp = Instant.now(),
                            sourceMessageId = message.id ?: "unknown"
                        )
                        logger.info("Updating existing memory for user {}: {} -> {}", message.senderId, fact.category, fact.value)
                        memoryFragmentRepository.save(updated)
                            .doOnNext {
                                if (it.confidence > 0.9) {
                                    publishPersonaUpdate(it)
                                }
                            }
                    }
                    .switchIfEmpty(
                        Mono.defer {
                            val fragment = MemoryFragment(
                                userId = message.senderId,
                                category = fact.category,
                                value = fact.value,
                                confidence = fact.confidence,
                                sourceMessageId = message.id ?: "unknown"
                            )
                            logger.info("Extracting new memory for user {}: {} -> {}", message.senderId, fact.category, fact.value)
                            memoryFragmentRepository.save(fragment)
                                .doOnNext {
                                    if (it.confidence > 0.9) {
                                        publishPersonaUpdate(it)
                                    }
                                }
                        }
                    )
                    .onErrorResume { e ->
                        // Handle DuplicateKeyException if concurrent writes happen
                        logger.warn("Potential race condition during memory extraction: {}", e.message)
                        Mono.empty()
                    }
            }
            .then()
    }

    private fun publishPersonaUpdate(fragment: MemoryFragment) {
        logger.info("Publishing high-confidence persona update for user {}: {} -> {}", fragment.userId, fragment.category, fragment.value)
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
    }
}
