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
        const val AI_EXCHANGE_NAME = "chat.ai.exchange"
        const val STORAGE_QUEUE_NAME = "chat.storage.queue"
        const val AI_REQUEST_QUEUE_NAME = "chat.ai.request.queue"
        const val AI_RESPONSE_QUEUE_NAME = "chat.ai.response.queue"
        const val ADAPTATION_EXCHANGE_NAME = "chat.adaptation.exchange"
        const val CONTENT_INJECTION_EXCHANGE_NAME = "chat.content.injection.exchange"
        const val PRESENCE_EXCHANGE_NAME = "chat.presence.exchange"
    }

    @Bean
    fun presenceExchange(): FanoutExchange {
        return FanoutExchange(PRESENCE_EXCHANGE_NAME)
    }

    @Bean
    fun presenceQueue(): Queue {
        return AnonymousQueue()
    }

    @Bean
    fun presenceBinding(presenceQueue: Queue, presenceExchange: FanoutExchange): Binding {
        return BindingBuilder.bind(presenceQueue).to(presenceExchange)
    }

    @Bean
    fun contentInjectionExchange(): FanoutExchange {
        return FanoutExchange(CONTENT_INJECTION_EXCHANGE_NAME)
    }

    @Bean
    fun contentInjectionQueue(): Queue {
        return AnonymousQueue()
    }

    @Bean
    fun contentInjectionBinding(contentInjectionQueue: Queue, contentInjectionExchange: FanoutExchange): Binding {
        return BindingBuilder.bind(contentInjectionQueue).to(contentInjectionExchange)
    }

    @Bean
    fun adaptationExchange(): FanoutExchange {
        return FanoutExchange(ADAPTATION_EXCHANGE_NAME)
    }

    @Bean
    fun adaptationQueue(): Queue {
        return AnonymousQueue()
    }

    @Bean
    fun adaptationBinding(adaptationQueue: Queue, adaptationExchange: FanoutExchange): Binding {
        return BindingBuilder.bind(adaptationQueue).to(adaptationExchange)
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
