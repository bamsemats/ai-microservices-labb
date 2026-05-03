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
    }

    @Bean
    fun presenceExchange(): FanoutExchange {
        return FanoutExchange(PRESENCE_EXCHANGE)
    }

    @Bean
    fun messageConverter(): MessageConverter {
        return Jackson2JsonMessageConverter()
    }
}
