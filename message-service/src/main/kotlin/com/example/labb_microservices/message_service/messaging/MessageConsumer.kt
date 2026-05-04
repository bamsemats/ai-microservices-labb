package com.example.labb_microservices.message_service.messaging

import com.example.labb_microservices.message_service.handler.MessageWebSocketHandler
import com.example.labb_microservices.message_service.model.AdaptationEvent
import com.example.labb_microservices.message_service.model.ContentInjectionEvent
import com.example.labb_microservices.message_service.model.Message
import com.example.labb_microservices.message_service.model.PresenceUpdateEvent
import com.example.labb_microservices.message_service.repository.MessageRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Service

@Service
class MessageConsumer(
    private val messageRepository: MessageRepository,
    private val webSocketHandler: MessageWebSocketHandler,
    private val objectMapper: ObjectMapper,
    private val messageProducer: MessageProducer
) {

    private val logger = LoggerFactory.getLogger(MessageConsumer::class.java)

    @RabbitListener(queues = [RabbitMQConfig.STORAGE_QUEUE_NAME])
    fun storeMessage(message: Message) {
        logger.info("Storing message: ${message.id}")
        messageRepository.save(message)
            .flatMap { savedMessage ->
                logger.info("Saved message to MongoDB: ${savedMessage.id}")
                messageProducer.deliverMessage(savedMessage)
                reactor.core.publisher.Mono.just(savedMessage)
            }
            .block()
    }

    @RabbitListener(queues = ["#{websocketQueue.name}"])
    fun deliverMessage(message: Message) {
        logger.info("Delivering message: ${message.id} to WebSockets in channel ${message.channelId}")
        try {
            val jsonMessage = objectMapper.writeValueAsString(message)
            
            if (message.receiverId == "all") {
                webSocketHandler.broadcastToChannel(message.channelId, jsonMessage)
            } else {
                val recipients = setOfNotNull(
                    message.receiverId.takeIf { it.isNotBlank() },
                    message.senderId.takeIf { it.isNotBlank() }
                )
                recipients.forEach { userId ->
                    webSocketHandler.sendMessageToUser(userId, message.channelId, jsonMessage)
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to serialize message for WebSocket", e)
            throw e
        }
    }

    @RabbitListener(queues = [RabbitMQConfig.AI_RESPONSE_QUEUE_NAME])
    fun consumeAiResponse(message: Message) {
        logger.info("Received AI response: ${message.id}")
        messageRepository.save(message)
            .flatMap { savedMessage ->
                logger.info("Saved AI message to MongoDB: ${savedMessage.id}")
                messageProducer.deliverMessage(savedMessage)
                reactor.core.publisher.Mono.just(savedMessage)
            }
            .block()
    }

    @RabbitListener(queues = ["#{adaptationQueue.name}"])
    fun consumeAdaptationEvent(event: AdaptationEvent) {
        logger.info("Received Adaptation Event: ${event.theme}")
        try {
            val jsonEvent = objectMapper.writeValueAsString(event)
            webSocketHandler.broadcastMessage(jsonEvent)
        } catch (e: Exception) {
            logger.error("Failed to serialize adaptation event for WebSocket", e)
            throw e
        }
    }

    @RabbitListener(queues = ["#{contentInjectionQueue.name}"])
    fun consumeContentInjectionEvent(event: ContentInjectionEvent) {
        logger.info("Received Content Injection Event: ${event.contentType}")
        try {
            val jsonEvent = objectMapper.writeValueAsString(event)
            webSocketHandler.broadcastMessage(jsonEvent)
        } catch (e: Exception) {
            logger.error("Failed to serialize content injection event for WebSocket", e)
            throw e
        }
    }

    @RabbitListener(queues = ["#{presenceQueue.name}"])
    fun consumePresenceUpdate(event: PresenceUpdateEvent) {
        logger.info("Received Presence Update: ${event.userId} is ${event.status}")
        try {
            val jsonEvent = objectMapper.writeValueAsString(event)
            // Broadcast presence to all users as it affects the sidebar
            webSocketHandler.broadcastMessage(jsonEvent)
        } catch (e: Exception) {
            logger.error("Failed to serialize presence update for WebSocket", e)
            throw e
        }
    }
}
