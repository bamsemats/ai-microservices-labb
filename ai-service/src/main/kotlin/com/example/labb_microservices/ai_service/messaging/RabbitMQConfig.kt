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
        const val STORAGE_EXCHANGE_NAME = "chat.storage.exchange"
        const val SENTIMENT_QUEUE_NAME = "chat.sentiment.queue"
        const val ADAPTATION_EXCHANGE_NAME = "chat.adaptation.exchange"
        const val ENTITY_EXCHANGE_NAME = "chat.entity.exchange"
        const val PERSONA_EXCHANGE_NAME = "chat.persona.exchange"
        const val PERSONA_UPDATE_QUEUE_NAME = "chat.persona.update.queue"
    }

    @Bean
    fun personaExchange(): DirectExchange {
        return DirectExchange(PERSONA_EXCHANGE_NAME)
    }

    @Bean
    fun personaUpdateQueue(): Queue {
        return Queue(PERSONA_UPDATE_QUEUE_NAME, true)
    }

    @Bean
    fun personaUpdateBinding(personaUpdateQueue: Queue, personaExchange: DirectExchange): Binding {
        return BindingBuilder.bind(personaUpdateQueue).to(personaExchange).with("persona.update")
    }

    @Bean
    fun entityExchange(): DirectExchange {
        return DirectExchange(ENTITY_EXCHANGE_NAME)
    }

    @Bean
    fun aiExchange(): DirectExchange {
        return DirectExchange(AI_EXCHANGE_NAME)
    }

    @Bean
    fun storageExchange(): DirectExchange {
        return DirectExchange(STORAGE_EXCHANGE_NAME)
    }

    @Bean
    fun adaptationExchange(): FanoutExchange {
        return FanoutExchange(ADAPTATION_EXCHANGE_NAME)
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
    fun sentimentQueue(): Queue {
        return Queue(SENTIMENT_QUEUE_NAME, true)
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
    fun sentimentBinding(sentimentQueue: Queue, storageExchange: DirectExchange): Binding {
        return BindingBuilder.bind(sentimentQueue).to(storageExchange).with("")
    }

    @Bean
    fun messageConverter(): MessageConverter {
        return Jackson2JsonMessageConverter()
    }
}
