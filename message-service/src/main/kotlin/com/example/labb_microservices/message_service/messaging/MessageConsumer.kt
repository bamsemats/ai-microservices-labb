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
    private val objectMapper: ObjectMapper,
    private val messageProducer: MessageProducer
) {

    private val logger = LoggerFactory.getLogger(MessageConsumer::class.java)

    @RabbitListener(queues = [RabbitMQConfig.STORAGE_QUEUE_NAME])
    fun storeMessage(message: Message) {
        logger.info("Storing message: \${message.id}")
        messageRepository.save(message)
            .subscribe { savedMessage ->
                logger.info("Saved message to MongoDB: \${savedMessage.id}")
                messageProducer.deliverMessage(savedMessage)
            }
    }

    @RabbitListener(queues = ["#{websocketQueue.name}"])
    fun deliverMessage(message: Message) {
        logger.info("Delivering message: \${message.id} to WebSockets")
        try {
            val jsonMessage = objectMapper.writeValueAsString(message)
            webSocketHandler.sendMessageToUser(message.receiverId, jsonMessage)
            webSocketHandler.sendMessageToUser(message.senderId, jsonMessage)
        } catch (e: Exception) {
            logger.error("Failed to serialize message for WebSocket", e)
        }
    }

}
