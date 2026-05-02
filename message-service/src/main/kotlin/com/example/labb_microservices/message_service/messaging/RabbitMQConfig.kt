package com.example.labb_microservices.message_service.messaging

import org.springframework.amqp.core.*
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.amqp.support.converter.MessageConverter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RabbitMQConfig {

    companion object {
        const val EXCHANGE_NAME = "chat.exchange"
        const val STORAGE_QUEUE_NAME = "chat.storage.queue"
    }

    @Bean
    fun exchange(): FanoutExchange {
        return FanoutExchange(EXCHANGE_NAME)
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
    fun storageBinding(storageQueue: Queue, exchange: FanoutExchange): Binding {
        return BindingBuilder.bind(storageQueue).to(exchange)
    }

    @Bean
    fun websocketBinding(websocketQueue: Queue, exchange: FanoutExchange): Binding {
        return BindingBuilder.bind(websocketQueue).to(exchange)
    }

    @Bean
    fun messageConverter(): MessageConverter {
        return Jackson2JsonMessageConverter()
    }
}
