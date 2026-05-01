package com.example.labb_microservices.message_service.messaging

import com.example.labb_microservices.message_service.model.Message
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Service

@Service
class MessageProducer(private val rabbitTemplate: RabbitTemplate) {

    fun sendMessage(message: Message) {
        rabbitTemplate.convertAndSend(
            RabbitMQConfig.EXCHANGE_NAME,
            RabbitMQConfig.ROUTING_KEY,
            message
        )
    }
}
