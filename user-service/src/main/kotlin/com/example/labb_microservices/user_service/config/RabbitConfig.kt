package com.example.labb_microservices.user_service.config

import com.fasterxml.jackson.databind.ObjectMapper
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
        const val PERSONA_DLX = "chat.persona.dlx"
        const val PERSONA_DLQ = "chat.persona.update.dlq"
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
    fun personaDLX(): TopicExchange {
        return TopicExchange(PERSONA_DLX)
    }

    @Bean
    fun personaDeadLetterQueue(): Queue {
        return Queue(PERSONA_DLQ, true)
    }

    @Bean
    fun personaDeadLetterBinding(personaDeadLetterQueue: Queue, personaDLX: TopicExchange): Binding {
        return BindingBuilder.bind(personaDeadLetterQueue).to(personaDLX).with(PERSONA_UPDATE_ROUTING_KEY)
    }

    @Bean
    fun personaUpdateQueue(): Queue {
        return QueueBuilder.durable(PERSONA_UPDATE_QUEUE)
            .withArgument("x-dead-letter-exchange", PERSONA_DLX)
            .withArgument("x-dead-letter-routing-key", PERSONA_UPDATE_ROUTING_KEY)
            .build()
    }

    @Bean
    fun personaUpdateBinding(personaUpdateQueue: Queue, personaExchange: TopicExchange): Binding {
        return BindingBuilder.bind(personaUpdateQueue).to(personaExchange).with(PERSONA_UPDATE_ROUTING_KEY)
    }

    @Bean
    fun messageConverter(objectMapper: ObjectMapper): MessageConverter {
        return Jackson2JsonMessageConverter(objectMapper)
    }
}
