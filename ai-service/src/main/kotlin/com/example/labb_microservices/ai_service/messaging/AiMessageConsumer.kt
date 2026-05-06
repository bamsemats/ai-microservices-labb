package com.example.labb_microservices.ai_service.messaging

import com.example.labb_microservices.ai_service.model.AiStatus
import com.example.labb_microservices.ai_service.model.AiStatusEvent
import com.example.labb_microservices.ai_service.model.AdaptationEvent
import com.example.labb_microservices.ai_service.model.AuthorType
import com.example.labb_microservices.ai_service.model.EntityMessage
import com.example.labb_microservices.ai_service.model.Message
import com.example.labb_microservices.ai_service.logic.AiReadinessIndicator
import com.example.labb_microservices.ai_service.logic.ResponseGenerator
import com.example.labb_microservices.ai_service.logic.MemoryWorker
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.util.*

@Service
class AiMessageConsumer(
    private val rabbitTemplate: RabbitTemplate,
    private val responseGenerator: ResponseGenerator,
    private val memoryWorker: MemoryWorker,
    private val readinessIndicator: AiReadinessIndicator
) {

    private val logger = LoggerFactory.getLogger(AiMessageConsumer::class.java)

    @RabbitListener(queues = [RabbitMQConfig.SENTIMENT_QUEUE_NAME])
    fun processSentimentAnalysis(message: Message) {
        if (message.authorType == AuthorType.BOT) return // Don't analyze self

        logger.info("Analyzing sentiment and entities for messageId: {}", message.id)
        
        // Memory Extraction - Non-blocking with timeout
        memoryWorker.processMessageForMemory(message)
            .timeout(java.time.Duration.ofSeconds(30))
            .subscribeOn(Schedulers.boundedElastic())
            .doOnError { e -> logger.error("Failed to extract memory from message: ${message.id} after timeout or error", e) }
            .subscribe()

        val content = message.content.lowercase()
        
        // Entity Extraction
        val entityTriggerMatch = Regex("(?:play(?:ing)?|watch(?:ing)?|stream(?:ing)?|video|youtube|tutorial)\\b\\s*([\\w\\s]+)", RegexOption.IGNORE_CASE).find(content)
        if (entityTriggerMatch != null) {
            val matchedVerb = entityTriggerMatch.value.split(Regex("\\s+"))[0].lowercase()
            val subject = entityTriggerMatch.groupValues[1].trim()
            
            val (type, value) = when {
                matchedVerb.startsWith("play") -> "GAME" to subject.replaceFirstChar { it.titlecase() }
                content.contains("elden ring") || subject.contains("elden ring") -> "GAME" to "Elden Ring"
                content.contains("valorant") || subject.contains("valorant") -> "GAME" to "Valorant"
                content.contains("minecraft") || subject.contains("minecraft") -> "GAME" to "Minecraft"
                content.contains("react") -> "VIDEO" to "React Tutorial"
                content.contains("python") -> "VIDEO" to "Python Tutorial"
                content.contains("kubernetes") -> "VIDEO" to "Kubernetes Tutorial"
                content.contains("lofi") || content.contains("music") -> "VIDEO" to "Lofi Hip Hop"
                subject.length > 2 -> (if (matchedVerb.startsWith("play")) "GAME" else "VIDEO") to subject.replaceFirstChar { it.titlecase() }
                else -> null to null
            }
            
            if (type != null && value != null) {
                logger.info("Entity detected: $type = $value")
                rabbitTemplate.convertAndSend(
                    RabbitMQConfig.ENTITY_EXCHANGE_NAME,
                    "entity.detected",
                    EntityMessage(
                        entityType = type,
                        entityValue = value,
                        originalMessageId = message.id ?: UUID.randomUUID().toString(),
                        senderId = message.senderId
                    )
                )
            }
        }

        // Sentiment Analysis
        val event = when {
            content.contains("urgent") || content.contains("critical") || content.contains("help") -> 
                AdaptationEvent(
                    theme = "emergency", 
                    intensity = 0.9, 
                    glowIntensity = 0.9,
                    color = "#f43f5e", 
                    primaryColor = "#f43f5e",
                    blurAmount = 24.0,
                    glassOpacity = 0.15
                )
            content.contains("happy") || content.contains("great") || content.contains("awesome") -> 
                AdaptationEvent(
                    theme = "vibrant", 
                    intensity = 0.8, 
                    glowIntensity = 0.8,
                    color = "#ec4899", 
                    primaryColor = "#ec4899",
                    blurAmount = 16.0,
                    glassOpacity = 0.08
                )
            content.contains("calm") || content.contains("relax") || content.contains("sleep") -> 
                AdaptationEvent(
                    theme = "zen", 
                    intensity = 0.2, 
                    glowIntensity = 0.2,
                    color = "#06b6d4", 
                    primaryColor = "#06b6d4",
                    blurAmount = 8.0,
                    glassOpacity = 0.02
                )
            content.contains("focus") || content.contains("work") -> 
                AdaptationEvent(
                    theme = "deep", 
                    intensity = 0.4, 
                    glowIntensity = 0.4,
                    color = "#8b5cf6", 
                    primaryColor = "#8b5cf6",
                    blurAmount = 20.0,
                    glassOpacity = 0.12
                )
            else -> null
        }

        event?.let {
            logger.info("Sentiment detected! Publishing adaptation event: ${it.theme}")
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.ADAPTATION_EXCHANGE_NAME,
                "",
                it
            )
        }
    }

    @RabbitListener(queues = [RabbitMQConfig.AI_REQUEST_QUEUE_NAME])
    fun processAiRequest(message: Message) {
        logger.info("Processing streaming AI request for messageId: {}", message.id)
        
        readinessIndicator.incrementActiveRequests()
        val responseId = UUID.randomUUID().toString()
        val receiverId = if (message.receiverId == "ai-bot") message.senderId else message.receiverId

        // Notify UI that AI is thinking
        try {
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.ADAPTATION_EXCHANGE_NAME,
                "",
                AiStatusEvent(
                    status = AiStatus.THINKING,
                    channelId = message.channelId,
                    userId = message.senderId
                )
            )
        } catch (e: Exception) {
            logger.error("Failed to publish THINKING status", e)
            readinessIndicator.decrementActiveRequests()
            throw e
        }

        responseGenerator.generateResponse(message)
            .timeout(java.time.Duration.ofSeconds(60))
            .concatMap { chunk ->
                Mono.fromRunnable<Unit> {
                    val aiChunk = Message(
                        id = responseId,
                        senderId = "ai-bot",
                        receiverId = receiverId,
                        channelId = message.channelId,
                        content = chunk,
                        authorType = AuthorType.BOT
                    )

                    rabbitTemplate.convertAndSend(
                        RabbitMQConfig.AI_EXCHANGE_NAME,
                        "ai.response",
                        aiChunk
                    )
                }
                .subscribeOn(Schedulers.boundedElastic())
                .thenReturn(chunk)
            }
            .doOnTerminate {
                readinessIndicator.decrementActiveRequests()
            }
            .then(
                Mono.fromRunnable<Unit> {
                    logger.info("AI streaming complete for responseId: {}", responseId)
                    // Notify UI that AI is done
                    rabbitTemplate.convertAndSend(
                        RabbitMQConfig.ADAPTATION_EXCHANGE_NAME,
                        "",
                        AiStatusEvent(
                            status = AiStatus.COMPLETED,
                            channelId = message.channelId,
                            userId = message.senderId
                        )
                    )
                }.subscribeOn(Schedulers.boundedElastic())
            )
            .onErrorResume { e ->
                Mono.fromRunnable<Unit> {
                    logger.error("AI processing failed", e)
                    rabbitTemplate.convertAndSend(
                        RabbitMQConfig.ADAPTATION_EXCHANGE_NAME,
                        "",
                        AiStatusEvent(
                            status = AiStatus.ERROR,
                            channelId = message.channelId,
                            userId = message.senderId
                        )
                    )
                }.subscribeOn(Schedulers.boundedElastic())
            }
            .block(java.time.Duration.ofSeconds(70))
    }
}
