package com.example.labb_microservices.message_service.messaging

import com.example.labb_microservices.message_service.handler.MessageWebSocketHandler
import com.example.labb_microservices.message_service.model.Message
import com.example.labb_microservices.message_service.repository.MessageRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Service

@Service
class MessageConsumer(
    private val messageRepository: MessageRepository,
    private val webSocketHandler: MessageWebSocketHandler,
    private val objectMapper: ObjectMapper
) {

    private val logger = LoggerFactory.getLogger(MessageConsumer::class.java)

    @RabbitListener(queues = [RabbitMQConfig.QUEUE_NAME])
    fun consumeMessage(message: Message) {
        logger.info("Consumed message: \${message.id} from \${message.senderId} to \${message.receiverId}")
        
        try {
            val jsonMessage = objectMapper.writeValueAsString(message)
            // Push to WebSocket
            webSocketHandler.sendMessageToUser(message.receiverId, jsonMessage)
            // Also push to sender for confirmation if needed, but here we assume multi-device or just simple delivery
            webSocketHandler.sendMessageToUser(message.senderId, jsonMessage)
        } catch (e: Exception) {
            logger.error("Failed to serialize message for WebSocket", e)
        }

        messageRepository.save(message)
            .subscribe { savedMessage ->
                logger.info("Saved message to MongoDB: \${savedMessage.id}")
            }
    }
}
