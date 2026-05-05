package com.example.labb_microservices.user_service.config

import org.springframework.amqp.core.FanoutExchange
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.amqp.support.converter.MessageConverter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RabbitConfig {

    companion object {
        const val PRESENCE_EXCHANGE = "chat.presence.exchange"
        const val PERSONA_UPDATE_QUEUE = "chat.persona.update.queue"
    }

    @Bean
    fun presenceExchange(): FanoutExchange {
        return FanoutExchange(PRESENCE_EXCHANGE)
    }

    @Bean
    fun personaUpdateQueue(): org.springframework.amqp.core.Queue {
        return org.springframework.amqp.core.Queue(PERSONA_UPDATE_QUEUE, true)
    }

    @Bean
    fun messageConverter(): MessageConverter {
        return Jackson2JsonMessageConverter()
    }
}
