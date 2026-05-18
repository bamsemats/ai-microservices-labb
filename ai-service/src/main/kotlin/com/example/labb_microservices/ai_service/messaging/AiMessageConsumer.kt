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
    private val readinessIndicator: AiReadinessIndicator,
    private val botRegistry: com.example.labb_microservices.ai_service.logic.BotRegistry,
    private val sentimentAnalyzer: com.example.labb_microservices.ai_service.logic.SentimentAnalyzer,
    private val sentimentStabilizer: com.example.labb_microservices.ai_service.logic.SentimentStabilizer
) {

    private val logger = LoggerFactory.getLogger(AiMessageConsumer::class.java)

    @RabbitListener(queues = [RabbitMQConfig.SENTIMENT_QUEUE_NAME])
    fun processSentimentAnalysis(message: Message) {
        if (message.authorType == AuthorType.BOT || botRegistry.isAiBot(message.senderId)) return

        logger.info("Analyzing semantic sentiment and entities for messageId: {}", message.id)
        
        // Memory Extraction
        val memoryPipeline = memoryWorker.processMessageForMemory(message)
            .timeout(java.time.Duration.ofSeconds(30))
            .subscribeOn(Schedulers.boundedElastic())
            .doOnError { e -> logger.error("Failed to extract memory from message: ${message.id}", e) }
            .onErrorResume { Mono.empty() }

        val content = message.content
        
        // Entity Extraction (Keyword-based for speed, can be upgraded later)
        val entityProcessing = Mono.fromRunnable<Unit> {
            val entityTriggerMatch = Regex("(?:play(?:ing)?|watch(?:ing)?|stream(?:ing)?|video|youtube|tutorial)\\b\\s*([\\w\\s]+)", RegexOption.IGNORE_CASE).find(content)
            if (entityTriggerMatch != null) {
                val matchedVerb = entityTriggerMatch.value.split(Regex("\\s+"))[0].lowercase()
                val rawSubject = entityTriggerMatch.groupValues[1].trim()
                val subject = sanitizeSubject(rawSubject)
                
                val (type, value) = when {
                    content.lowercase().contains("elden ring") -> "GAME" to "Elden Ring"
                    content.lowercase().contains("valorant") -> "GAME" to "Valorant"
                    content.lowercase().contains("minecraft") -> "GAME" to "Minecraft"
                    matchedVerb.startsWith("play") -> "GAME" to subject.replaceFirstChar { it.titlecase() }
                    content.lowercase().contains("react") -> "VIDEO" to "React Tutorial"
                    content.lowercase().contains("python") -> "VIDEO" to "Python Tutorial"
                    content.lowercase().contains("kubernetes") -> "VIDEO" to "Kubernetes Tutorial"
                    content.lowercase().contains("lofi") || content.lowercase().contains("music") -> "VIDEO" to "Lofi Hip Hop"
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

        // Semantic Sentiment Analysis
        val sentimentProcessing = sentimentAnalyzer.analyzeSentiment(content)
            .flatMap { event ->
                if (event != null && sentimentStabilizer.shouldPublish(message.channelId ?: "global", event)) {
                    Mono.fromRunnable<Unit> {
                        logger.info("Semantic sentiment detected! Theme: ${event.theme}, Intensity: ${event.intensity}")
                        rabbitTemplate.convertAndSend(
                            RabbitMQConfig.ADAPTATION_EXCHANGE_NAME,
                            "",
                            event
                        )
                    }
                } else {
                    Mono.empty()
                }
            }
            .subscribeOn(Schedulers.boundedElastic())
            .onErrorResume { e -> 
                logger.error("Sentiment analysis failed", e)
                Mono.empty()
            }

        Mono.`when`(memoryPipeline, entityProcessing, sentimentProcessing).then().block(java.time.Duration.ofSeconds(60))
    }


    private fun sanitizeSubject(subject: String): String {
        return subject.replace(Regex("(?:\\b(?:at|tonight|am|pm|the|a|an)\\b|\\d{1,2}(?::\\d{2})?)", RegexOption.IGNORE_CASE), "").trim()
    }

    companion object {
        private val AI_MENTION_REGEX = Regex("(?i)(?:^|\\W)@(ai-bot|ai|adaptaai|nexusprime|echoflow|vibecheck|helpdesk)(?:\\W|$)")
    }

    @RabbitListener(queues = [RabbitMQConfig.AI_REQUEST_QUEUE_NAME])
    fun processAiRequest(message: Message) {
        logger.info("Processing streaming AI request for messageId: {}", message.id)
        
        readinessIndicator.incrementActiveRequests()
        val responseId = UUID.randomUUID().toString()
        
        // Dynamic bot identification
        val isExplicitReceiver = botRegistry.isAiBot(message.receiverId)
        val mentionedBotId = AI_MENTION_REGEX.find(message.content)?.groupValues?.get(1)?.lowercase()
        val targetBotId = if (isExplicitReceiver) {
            botRegistry.getBotId(message.receiverId)
        } else if (mentionedBotId != null && botRegistry.isAiBot(mentionedBotId)) {
            botRegistry.getBotId(mentionedBotId)
        } else {
            botRegistry.getBotId("ai-bot") // Fallback
        }
        
        val botName = botRegistry.getBotDisplayName(targetBotId)
        
        val receiverId = if (message.receiverId == "all") "all" else message.senderId

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
                        senderId = targetBotId,
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
