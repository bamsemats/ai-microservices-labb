package com.example.labb_microservices.ai_service.messaging

import com.example.labb_microservices.ai_service.model.AdaptationEvent
import com.example.labb_microservices.ai_service.model.AuthorType
import com.example.labb_microservices.ai_service.model.EntityMessage
import com.example.labb_microservices.ai_service.model.Message
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Service
import java.util.*

@Service
class AiMessageConsumer(private val rabbitTemplate: RabbitTemplate) {

    private val logger = LoggerFactory.getLogger(AiMessageConsumer::class.java)

    @RabbitListener(queues = [RabbitMQConfig.SENTIMENT_QUEUE_NAME])
    fun processSentimentAnalysis(message: Message) {
        if (message.authorType == AuthorType.BOT) return // Don't analyze self

        logger.info("Analyzing sentiment and entities for message: ${message.content}")
        val content = message.content.lowercase()
        
        // Entity Extraction
        if (content.contains("playing") || content.contains("stream") || content.contains("watch") || 
            content.contains("video") || content.contains("youtube") || content.contains("tutorial")) {
            
            val (type, value) = when {
                content.contains("elden ring") -> "GAME" to "Elden Ring"
                content.contains("valorant") -> "GAME" to "Valorant"
                content.contains("minecraft") -> "GAME" to "Minecraft"
                content.contains("react") || content.contains("tutorial") -> "VIDEO" to "React Tutorial"
                content.contains("music") || content.contains("youtube") -> "VIDEO" to "Lofi Hip Hop"
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
                AdaptationEvent(theme = "emergency", intensity = 0.9, color = "#f43f5e")
            content.contains("happy") || content.contains("great") || content.contains("awesome") -> 
                AdaptationEvent(theme = "vibrant", intensity = 0.8, color = "#ec4899")
            content.contains("calm") || content.contains("relax") || content.contains("sleep") -> 
                AdaptationEvent(theme = "zen", intensity = 0.2, color = "#06b6d4")
            content.contains("focus") || content.contains("work") -> 
                AdaptationEvent(theme = "deep", intensity = 0.4, color = "#8b5cf6")
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
        logger.info("Processing AI request from ${message.senderId}: ${message.content}")
        
        // Simulate AI processing
        val aiResponseContent = "Hello ${message.senderId}, I received your message: '${message.content}'. This is an automated AI response."
        
        val aiMessage = Message(
            id = UUID.randomUUID().toString(),
            senderId = "ai-bot",
            receiverId = if (message.receiverId == "ai-bot") message.senderId else message.receiverId,
            content = aiResponseContent,
            authorType = AuthorType.BOT
        )

        logger.info("Sending AI response back to chat")
        rabbitTemplate.convertAndSend(
            RabbitMQConfig.AI_EXCHANGE_NAME,
            "ai.response",
            aiMessage
        )
    }
}
