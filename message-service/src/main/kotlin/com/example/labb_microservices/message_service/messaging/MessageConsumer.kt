package com.example.labb_microservices.message_service.messaging

import com.example.labb_microservices.message_service.handler.MessageWebSocketHandler
import com.example.labb_microservices.message_service.model.Message
import com.example.labb_microservices.message_service.repository.MessageRepository
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Service

@Service
class MessageConsumer(
    private val messageRepository: MessageRepository,
    private val webSocketHandler: MessageWebSocketHandler
) {

    private val logger = LoggerFactory.getLogger(MessageConsumer::class.java)

    @RabbitListener(queues = [RabbitMQConfig.QUEUE_NAME])
    fun consumeMessage(message: Message) {
        logger.info("Consumed message: \${message.id} from \${message.senderId} to \${message.receiverId}")
        
        // Push to WebSocket
        webSocketHandler.sendMessageToUser(message.receiverId, "From \${message.senderId}: \${message.content}")

        messageRepository.save(message)
            .subscribe { savedMessage ->
                logger.info("Saved message to MongoDB: \${savedMessage.id}")
            }
    }
}
