package com.example.labb_microservices.ai_service.messaging

import com.example.labb_microservices.ai_service.model.AiStatus
import com.example.labb_microservices.ai_service.model.AiStatusEvent
import com.example.labb_microservices.ai_service.model.AuthorType
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
        
        // Semantic Sentiment and Entity Analysis
        val sentimentProcessing = sentimentAnalyzer.analyzeSentiment(content)
            .flatMap { event ->
                if (event != null) {
                    val enrichedEvent = event.copy(messageId = message.id)
                    
                    Mono.fromRunnable<Unit> {
                        // 1. Check for adaptation publication
                        if (sentimentStabilizer.shouldPublish(message.channelId, enrichedEvent)) {
                            logger.info("Semantic sentiment detected! Theme: ${enrichedEvent.theme}, Intensity: ${enrichedEvent.intensity}")
                            rabbitTemplate.convertAndSend(
                                RabbitMQConfig.ADAPTATION_EXCHANGE_NAME,
                                "",
                                enrichedEvent
                            )
                        }

                        // 2. Process and dispatch semantic entities
                        enrichedEvent.entities?.forEach { entity ->
                            val enrichedEntity = entity.copy(
                                originalMessageId = message.id ?: UUID.randomUUID().toString(),
                                senderId = message.senderId,
                                channelId = message.channelId
                            )
                            logger.info("Semantic entity detected via AI: ${enrichedEntity.entityType} = ${enrichedEntity.entityValue} (conf: ${enrichedEntity.confidence})")
                            rabbitTemplate.convertAndSend(
                                RabbitMQConfig.ENTITY_EXCHANGE_NAME,
                                "entity.detected",
                                enrichedEntity
                            )
                        }
                    }.subscribeOn(Schedulers.boundedElastic()).then(Mono.just(Unit))
                } else {
                    Mono.empty()
                }
            }
            .subscribeOn(Schedulers.boundedElastic())
            .onErrorResume { e -> 
                logger.error("Sentiment/Entity analysis failed", e)
                Mono.empty()
            }

        Mono.`when`(memoryPipeline, sentimentProcessing).then().block(java.time.Duration.ofSeconds(60))
    }

    companion object {
        private val AI_MENTION_REGEX = Regex("(?i)(?:^|\\W)@(ai-bot|ai|adaptaai|nexusprime|echoflow|vibecheck|helpdesk)(?:\\W|$)")
    }

    @RabbitListener(queues = [RabbitMQConfig.AI_REQUEST_QUEUE_NAME])
    fun processAiRequest(message: Message) {
        logger.info("[TRACE] Received AI request - id: {}, sender: {}, receiver: {}, channel: {}", 
            message.id, message.senderId, message.receiverId, message.channelId)
        
        readinessIndicator.incrementActiveRequests()
        val responseId = UUID.randomUUID().toString()
        
        // Dynamic bot identification
        val mentionedBotId = AI_MENTION_REGEX.find(message.content)?.groupValues?.get(1)
        val targetBotId = when {
            botRegistry.isAiBot(message.receiverId) -> botRegistry.getBotId(message.receiverId)
            mentionedBotId != null && botRegistry.isAiBot(mentionedBotId) -> botRegistry.getBotId(mentionedBotId)
            else -> "ai-bot"
        }
        
        val botName = botRegistry.getBotDisplayName(targetBotId)
        logger.info("[TRACE] Responding as bot: {} ({}) for user: {}", targetBotId, botName, message.senderId)
        
        // If it's a broadcast channel (general/global) or receiver was "all", respond to "all"
        val isBroadcastChannel = message.channelId.lowercase() in setOf("general", "global", "home")
        val receiverId = if (message.receiverId == "all" || isBroadcastChannel) "all" else message.senderId
        val channelId = if (isBroadcastChannel) "general" else message.channelId

        // Notify UI that AI is thinking
        val startNotify = Mono.fromRunnable<Unit> {
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.ADAPTATION_EXCHANGE_NAME,
                "",
                AiStatusEvent(
                    status = AiStatus.THINKING,
                    channelId = channelId,
                    userId = message.senderId
                )
            )
        }.subscribeOn(Schedulers.boundedElastic())

        startNotify.thenMany(responseGenerator.generateResponse(message))
            .timeout(java.time.Duration.ofSeconds(60))
            .collectList()
            .map { chunks ->
                chunks.joinToString("").ifBlank {
                    "Interference detected in the frequency. Please try again later."
                }
            }
            .flatMap { fullContent ->
                Mono.fromRunnable<Unit> {
                    val aiMessage = Message(
                        id = responseId,
                        senderId = targetBotId,
                        senderName = botName,
                        receiverId = receiverId,
                        channelId = channelId,
                        content = fullContent,
                        authorType = AuthorType.BOT
                    )

                    logger.info("[TRACE] Sending AI response {} to RabbitMQ via {}", responseId, RabbitMQConfig.AI_EXCHANGE_NAME)
                    rabbitTemplate.convertAndSend(
                        RabbitMQConfig.AI_EXCHANGE_NAME,
                        "ai.response",
                        aiMessage
                    )
                    logger.info("[TRACE] AI response {} successfully dispatched", responseId)
                }.subscribeOn(Schedulers.boundedElastic())
            }
            .doOnTerminate {
                readinessIndicator.decrementActiveRequests()
            }
            .then(
                Mono.fromRunnable<Unit> {
                    logger.info("AI generation complete for responseId: {}", responseId)
                    // Notify UI that AI is done
                    rabbitTemplate.convertAndSend(
                        RabbitMQConfig.ADAPTATION_EXCHANGE_NAME,
                        "",
                        AiStatusEvent(
                            status = AiStatus.COMPLETED,
                            channelId = channelId,
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
                            channelId = channelId,
                            userId = message.senderId
                        )
                    )
                }.subscribeOn(Schedulers.boundedElastic())
            }
            .then()
            .block(java.time.Duration.ofSeconds(70))
    }
}
