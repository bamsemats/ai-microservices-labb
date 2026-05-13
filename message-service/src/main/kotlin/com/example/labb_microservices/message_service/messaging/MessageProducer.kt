package com.example.labb_microservices.message_service.messaging

import com.example.labb_microservices.message_service.model.Message
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Service

@Service
class MessageProducer(private val rabbitTemplate: RabbitTemplate) {

    fun sendMessage(message: Message) {
        rabbitTemplate.convertAndSend(
            RabbitMQConfig.STORAGE_EXCHANGE_NAME,
            "message-published",
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

    fun sendSentimentRequest(message: Message) {
        rabbitTemplate.convertAndSend(
            RabbitMQConfig.STORAGE_EXCHANGE_NAME,
            "sentiment",
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

    fun broadcastEvent(event: Any) {
        rabbitTemplate.convertAndSend(
            RabbitMQConfig.ADAPTATION_EXCHANGE_NAME,
            "",
            event
        )
    }

    fun storeEvent(event: Any) {
        rabbitTemplate.convertAndSend(
            RabbitMQConfig.STORAGE_EXCHANGE_NAME,
            "event.storage",
            event
        )
    }
}
