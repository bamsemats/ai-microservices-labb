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
        if (message.authorType == AuthorType.BOT) return

        logger.info("Analyzing sentiment and entities for messageId: {}", message.id)
        
        // Memory Extraction - Composed into the pipeline
        val memoryPipeline = memoryWorker.processMessageForMemory(message)
            .timeout(java.time.Duration.ofSeconds(30))
            .subscribeOn(Schedulers.boundedElastic())
            .doOnError { e -> logger.error("Failed to extract memory from message: ${message.id} after timeout or error", e) }
            .onErrorResume { Mono.empty() } // Don't fail the whole pipeline for memory extraction

        val content = message.content.lowercase()
        
        // Entity Extraction
        val entityProcessing = Mono.fromRunnable<Unit> {
            val entityTriggerMatch = Regex("(?:play(?:ing)?|watch(?:ing)?|stream(?:ing)?|video|youtube|tutorial)\\b\\s*([\\w\\s]+)", RegexOption.IGNORE_CASE).find(content)
            if (entityTriggerMatch != null) {
                val matchedVerb = entityTriggerMatch.value.split(Regex("\\s+"))[0].lowercase()
                val rawSubject = entityTriggerMatch.groupValues[1].trim()
                val subject = sanitizeSubject(rawSubject)
                
                val (type, value) = when {
                    matchedVerb.startsWith("play") -> "GAME" to subject.replaceFirstChar { it.titlecase() }
                    content.contains("elden ring") || subject.contains("elden ring") -> "GAME" to "Elden Ring"
                    content.contains("valorant") || subject.contains("valorant") -> "GAME" to "Valorant"
                    content.contains("minecraft") || subject.contains("minecraft") -> "GAME" to "Minecraft"
                    content.contains("react") -> "VIDEO" to "React Tutorial"
                    content.contains("python") -> "VIDEO" to "Python Tutorial"
                    content.contains("kubernetes") -> "VIDEO" to "Kubernetes Tutorial"
                    content.contains("lofi") || content.contains("music") -> "VIDEO" to "Lofi Hip Hop"
                    subject.length > 2 && subject.length < 50 && rawSubject.firstOrNull()?.isUpperCase() == true -> (if (matchedVerb.startsWith("play")) "GAME" else "VIDEO") to subject.replaceFirstChar { it.titlecase() }
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
        }.subscribeOn(Schedulers.boundedElastic())

        // Sentiment Analysis
        val sentimentProcessing = Mono.fromRunnable<Unit> {
            val event = when {
                Regex("\\b(urgent|critical|help|emergency|911)\\b", RegexOption.IGNORE_CASE).containsMatchIn(content) -> 
                    AdaptationEvent(
                        theme = "emergency", 
                        intensity = 0.9, 
                        glowIntensity = 0.9,
                        color = "#f43f5e", 
                        blurAmount = 24.0,
                        glassOpacity = 0.15
                    )
                Regex("\\b(happy|great|awesome|joy|excited)\\b", RegexOption.IGNORE_CASE).containsMatchIn(content) -> 
                    AdaptationEvent(
                        theme = "vibrant", 
                        intensity = 0.8, 
                        glowIntensity = 0.8,
                        color = "#ec4899", 
                        blurAmount = 16.0,
                        glassOpacity = 0.08
                    )
                Regex("\\b(calm|relax|sleep|peaceful|zen)\\b", RegexOption.IGNORE_CASE).containsMatchIn(content) -> 
                    AdaptationEvent(
                        theme = "zen", 
                        intensity = 0.2, 
                        glowIntensity = 0.2,
                        color = "#06b6d4", 
                        blurAmount = 8.0,
                        glassOpacity = 0.02
                    )
                Regex("\\b(focus|work|study|job|deep)\\b", RegexOption.IGNORE_CASE).containsMatchIn(content) -> 
                    AdaptationEvent(
                        theme = "deep", 
                        intensity = 0.4, 
                        glowIntensity = 0.4,
                        color = "#8b5cf6", 
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
        }.subscribeOn(Schedulers.boundedElastic())

        Mono.`when`(memoryPipeline, entityProcessing, sentimentProcessing).then().block(java.time.Duration.ofSeconds(60))
    }

    private fun sanitizeSubject(subject: String): String {
        return subject.replace(Regex("(?:\\b(?:at|tonight|am|pm|the|a|an)\\b|\\d{1,2}(?::\\d{2})?)", RegexOption.IGNORE_CASE), "").trim()
    }

    @RabbitListener(queues = [RabbitMQConfig.AI_REQUEST_QUEUE_NAME])
    fun processAiRequest(message: Message) {
        logger.info("Processing streaming AI request for messageId: {}", message.id)
        
        readinessIndicator.incrementActiveRequests()
        val responseId = UUID.randomUUID().toString()
        
        // Dynamic bot identification
        val livingBots = listOf("ai-bot", "AdaptaAI", "NexusPrime", "EchoFlow", "VibeCheck", "HelpDesk")
        val isAiBot = message.receiverId in livingBots
        val botId = if (isAiBot) message.receiverId else "ai-bot"
        val botName = when (botId) {
            "NexusPrime" -> "NexusPrime (Architect)"
            "AdaptaAI" -> "AdaptaAI (Assistant)"
            "EchoFlow" -> "EchoFlow (Curator)"
            "VibeCheck" -> "VibeCheck (Moderator)"
            "HelpDesk" -> "HelpDesk (Support)"
            else -> "AdaptaChat AI"
        }
        
        val receiverId = if (isAiBot) message.senderId else message.receiverId

        // Notify UI that AI is thinking
        val startNotify = Mono.fromRunnable<Unit> {
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.ADAPTATION_EXCHANGE_NAME,
                "",
                AiStatusEvent(
                    status = AiStatus.THINKING,
                    channelId = message.channelId,
                    userId = message.senderId
                )
            )
        }.subscribeOn(Schedulers.boundedElastic())

        startNotify.thenMany(responseGenerator.generateResponse(message))
            .timeout(java.time.Duration.ofSeconds(60))
            .concatMap { chunk ->
                Mono.fromRunnable<Unit> {
                    val aiChunk = Message(
                        id = responseId,
                        senderId = botId,
                        senderName = botName,
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
            .then()
            .block(java.time.Duration.ofSeconds(70))
    }
}
