package com.example.labb_microservices.user_service.config

import org.springframework.amqp.core.*
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.amqp.support.converter.MessageConverter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RabbitConfig {

    companion object {
        const val PRESENCE_EXCHANGE = "chat.presence.exchange"
        const val PERSONA_EXCHANGE = "chat.persona.exchange"
        const val PERSONA_UPDATE_QUEUE = "chat.persona.update.queue"
        const val PERSONA_UPDATE_ROUTING_KEY = "persona.update"
    }

    @Bean
    fun presenceExchange(): FanoutExchange {
        return FanoutExchange(PRESENCE_EXCHANGE)
    }

    @Bean
    fun personaExchange(): TopicExchange {
        return TopicExchange(PERSONA_EXCHANGE)
    }

    @Bean
    fun personaUpdateQueue(): Queue {
        return Queue(PERSONA_UPDATE_QUEUE, true)
    }

    @Bean
    fun personaUpdateBinding(personaUpdateQueue: Queue, personaExchange: TopicExchange): Binding {
        return BindingBuilder.bind(personaUpdateQueue).to(personaExchange).with(PERSONA_UPDATE_ROUTING_KEY)
    }

    @Bean
    fun messageConverter(): MessageConverter {
        return Jackson2JsonMessageConverter()
    }
}
