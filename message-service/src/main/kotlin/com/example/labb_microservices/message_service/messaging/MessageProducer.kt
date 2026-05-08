package com.example.labb_microservices.message_service.messaging

import com.example.labb_microservices.message_service.model.Message
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Service

@Service
class MessageProducer(private val rabbitTemplate: RabbitTemplate) {

    fun sendMessage(message: Message) {
        rabbitTemplate.convertAndSend(
            RabbitMQConfig.STORAGE_EXCHANGE_NAME,
            "",
            message
        )
    }

    fun deliverMessage(message: Message) {
        rabbitTemplate.convertAndSend(
            RabbitMQConfig.DELIVERY_EXCHANGE_NAME,
            "",
            message
        )
    }

    fun sendAiRequest(message: Message) {
        rabbitTemplate.convertAndSend(
            RabbitMQConfig.AI_EXCHANGE_NAME,
            "ai.request",
            message
        )
    }
}
