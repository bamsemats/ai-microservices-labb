package com.example.labb_microservices.ai_service.messaging

import com.example.labb_microservices.ai_service.model.AuthorType
import com.example.labb_microservices.ai_service.model.Message
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Service
import java.util.*

@Service
class AiMessageConsumer(private val rabbitTemplate: RabbitTemplate) {

    private val logger = LoggerFactory.getLogger(AiMessageConsumer::class.java)

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
