package com.example.labb_microservices.content_aggregator.messaging

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.amqp.core.*
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.amqp.support.converter.MessageConverter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RabbitMQConfig {

    companion object {
        const val ENTITY_EXCHANGE_NAME = "chat.entity.exchange"
        const val ENTITY_QUEUE_NAME = "chat.entity.queue"
        const val CONTENT_INJECTION_EXCHANGE_NAME = "chat.content.injection.exchange"
        const val DELIVERY_EXCHANGE_NAME = "chat.delivery.exchange"
        const val AI_EXCHANGE_NAME = "chat.ai.exchange"
        const val AI_RESPONSE_QUEUE_NAME = "chat.ai.response.shared.queue"

        const val ENTITY_ROUTING_KEY = "entity.detected"
        const val AI_RESPONSE_ROUTING_KEY = "ai.response"
    }

    @Bean
    fun aiExchange(): DirectExchange {
        return DirectExchange(AI_EXCHANGE_NAME)
    }

    @Bean
    fun aiResponseQueue(): Queue {
        return Queue(AI_RESPONSE_QUEUE_NAME, true)
    }

    @Bean
    fun aiResponseBinding(aiResponseQueue: Queue, aiExchange: DirectExchange): Binding {
        return BindingBuilder.bind(aiResponseQueue).to(aiExchange).with(AI_RESPONSE_ROUTING_KEY)
    }

    @Bean
    fun entityExchange(): DirectExchange {
        return DirectExchange(ENTITY_EXCHANGE_NAME)
    }

    @Bean
    fun contentInjectionExchange(): FanoutExchange {
        return FanoutExchange(CONTENT_INJECTION_EXCHANGE_NAME)
    }

    @Bean
    fun entityQueue(): Queue {
        return Queue(ENTITY_QUEUE_NAME, true)
    }

    @Bean
    fun entityBinding(entityQueue: Queue, entityExchange: DirectExchange): Binding {
        return BindingBuilder.bind(entityQueue).to(entityExchange).with(ENTITY_ROUTING_KEY)
    }

    @Bean
    fun deliveryExchange(): FanoutExchange {
        return FanoutExchange(DELIVERY_EXCHANGE_NAME)
    }

    @Bean
    fun deliveryQueue(): Queue {
        return AnonymousQueue()
    }

    @Bean
    fun deliveryBinding(deliveryQueue: Queue, deliveryExchange: FanoutExchange): Binding {
        return BindingBuilder.bind(deliveryQueue).to(deliveryExchange)
    }

    @Bean
    fun messageConverter(objectMapper: ObjectMapper): MessageConverter {
        return Jackson2JsonMessageConverter(objectMapper)
    }
}
