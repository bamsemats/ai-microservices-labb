package com.example.labb_microservices.ai_service.messaging

import org.springframework.amqp.core.*
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.amqp.support.converter.MessageConverter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RabbitMQConfig {

    companion object {
        const val AI_EXCHANGE_NAME = "chat.ai.exchange"
        const val AI_REQUEST_QUEUE_NAME = "chat.ai.request.queue"
        const val AI_RESPONSE_QUEUE_NAME = "chat.ai.response.queue"
    }

    @Bean
    fun aiExchange(): DirectExchange {
        return DirectExchange(AI_EXCHANGE_NAME)
    }

    @Bean
    fun aiRequestQueue(): Queue {
        return Queue(AI_REQUEST_QUEUE_NAME, true)
    }

    @Bean
    fun aiResponseQueue(): Queue {
        return Queue(AI_RESPONSE_QUEUE_NAME, true)
    }

    @Bean
    fun aiRequestBinding(aiRequestQueue: Queue, aiExchange: DirectExchange): Binding {
        return BindingBuilder.bind(aiRequestQueue).to(aiExchange).with("ai.request")
    }

    @Bean
    fun aiResponseBinding(aiResponseQueue: Queue, aiExchange: DirectExchange): Binding {
        return BindingBuilder.bind(aiResponseQueue).to(aiExchange).with("ai.response")
    }

    @Bean
    fun messageConverter(): MessageConverter {
        return Jackson2JsonMessageConverter()
    }
}
