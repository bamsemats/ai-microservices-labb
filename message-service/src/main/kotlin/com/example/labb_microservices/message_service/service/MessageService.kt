package com.example.labb_microservices.message_service.service

import com.example.labb_microservices.message_service.messaging.MessageProducer
import com.example.labb_microservices.message_service.model.Message
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers

@Service
class MessageService(
    private val messageProducer: MessageProducer
) {
    private val logger = LoggerFactory.getLogger(MessageService::class.java)

    private val AI_BOT_IDS = setOf("ai", "ai-bot", "adaptaai", "nexusprime", "echoflow", "vibecheck", "helpdesk")
    private val AI_MENTION_REGEX = Regex("(?i)(?:^|\\W)@(ai-bot|ai|adaptaai|nexusprime|echoflow|vibecheck|helpdesk)(?:\\W|$)")

    fun processMessage(message: Message): Mono<Void> {
        val sanitizedChannelId = if (message.channelId == "home" || message.channelId == "all") "general" else message.channelId
        val sanitizedReceiverId = if (message.receiverId == "home") "all" else message.receiverId
        
        val normalizedMessage = message.copy(
            channelId = sanitizedChannelId,
            receiverId = sanitizedReceiverId
        )

        return Mono.fromCallable {
            logger.info("[TRACE] processMessage START - id: {}, sender: {}, senderName: {}, receiver: {}, channel: {}", 
                normalizedMessage.id, normalizedMessage.senderId, normalizedMessage.senderName, normalizedMessage.receiverId, normalizedMessage.channelId)
            
            // 1. Send to storage queue
            messageProducer.sendMessage(normalizedMessage)
            logger.info("[TRACE] Sent message {} to storage queue", normalizedMessage.id)
            
            // 2. Check for AI triggers
            val isAiRecipient = AI_BOT_IDS.contains(normalizedMessage.receiverId.lowercase())
            val hasAiMention = AI_MENTION_REGEX.containsMatchIn(normalizedMessage.content)
            
            if (isAiRecipient || hasAiMention) {
                logger.info("[TRACE] AI Trigger DETECTED for message {}. isAiRecipient={}, hasAiMention={}", 
                    normalizedMessage.id, isAiRecipient, hasAiMention)
                try {
                    messageProducer.sendAiRequest(normalizedMessage)
                    logger.info("[TRACE] AI request sent to RabbitMQ for message {}", normalizedMessage.id)
                } catch (e: Exception) {
                    logger.error("[TRACE] FAILED to send AI request to RabbitMQ", e)
                }
            } else {
                logger.info("[TRACE] No AI trigger for message {}", normalizedMessage.id)
            }
        }.subscribeOn(Schedulers.boundedElastic()).then()
    }
}
