package com.example.labb_microservices.message_service.messaging

import org.springframework.amqp.core.*
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.amqp.support.converter.MessageConverter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RabbitMQConfig {

    companion object {
        const val STORAGE_EXCHANGE_NAME = "chat.storage.exchange"
        const val DELIVERY_EXCHANGE_NAME = "chat.delivery.exchange"
        const val STORAGE_QUEUE_NAME = "chat.storage.queue"
    }

    @Bean
    fun storageExchange(): DirectExchange {
        return DirectExchange(STORAGE_EXCHANGE_NAME)
    }

    @Bean
    fun deliveryExchange(): FanoutExchange {
        return FanoutExchange(DELIVERY_EXCHANGE_NAME)
    }

    @Bean
    fun storageQueue(): Queue {
        return Queue(STORAGE_QUEUE_NAME, true)
    }

    @Bean
    fun websocketQueue(): Queue {
        return AnonymousQueue()
    }

    @Bean
    fun storageBinding(storageQueue: Queue, storageExchange: DirectExchange): Binding {
        return BindingBuilder.bind(storageQueue).to(storageExchange).with("")
    }

    @Bean
    fun websocketBinding(websocketQueue: Queue, deliveryExchange: FanoutExchange): Binding {
        return BindingBuilder.bind(websocketQueue).to(deliveryExchange)
    }

    @Bean
    fun messageConverter(): MessageConverter {
        return Jackson2JsonMessageConverter()
    }
}
