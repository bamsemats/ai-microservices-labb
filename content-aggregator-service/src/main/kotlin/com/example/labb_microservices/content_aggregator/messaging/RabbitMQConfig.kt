package com.example.labb_microservices.content_aggregator.messaging

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
    }

    @Bean
    fun aiExchange(): DirectExchange {
        return DirectExchange(AI_EXCHANGE_NAME)
    }

    @Bean
    fun aiResponseQueue(): Queue {
        return AnonymousQueue()
    }

    @Bean
    fun aiResponseBinding(aiResponseQueue: Queue, aiExchange: DirectExchange): Binding {
        return BindingBuilder.bind(aiResponseQueue).to(aiExchange).with("ai.response")
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
        return BindingBuilder.bind(entityQueue).to(entityExchange).with("entity.detected")
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
    fun messageConverter(): MessageConverter {
        return Jackson2JsonMessageConverter()
    }
}
