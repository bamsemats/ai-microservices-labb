package com.example.labb_microservices.ai_service.messaging

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.amqp.core.*
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitAdmin
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
        const val ENTITY_QUEUE_NAME = "chat.entity.queue"

        const val PERSONA_EXCHANGE_NAME = "chat.persona.exchange"
        const val PERSONA_UPDATE_QUEUE_NAME = "chat.persona.update.queue"
    }

    // =========================
    // Exchanges
    // =========================

    // 🔥 FIX: Ska vara TopicExchange (inte Direct)
    @Bean
    fun personaExchange(): TopicExchange =
        TopicExchange(PERSONA_EXCHANGE_NAME)

    @Bean
    fun entityExchange(): DirectExchange =
        DirectExchange(ENTITY_EXCHANGE_NAME)

    @Bean
    fun aiExchange(): DirectExchange =
        DirectExchange(AI_EXCHANGE_NAME)

    @Bean
    fun storageExchange(): DirectExchange =
        DirectExchange(STORAGE_EXCHANGE_NAME)

    @Bean
    fun adaptationExchange(): FanoutExchange =
        FanoutExchange(ADAPTATION_EXCHANGE_NAME)

    // =========================
    // Queues
    // =========================

    @Bean
    fun aiRequestQueue(): Queue =
        Queue(AI_REQUEST_QUEUE_NAME, true)

    @Bean
    fun aiResponseQueue(): Queue =
        Queue(AI_RESPONSE_QUEUE_NAME, true)

    @Bean
    fun sentimentQueue(): Queue =
        Queue(SENTIMENT_QUEUE_NAME, true)

    @Bean
    fun entityQueue(): Queue =
        Queue(ENTITY_QUEUE_NAME, true)

    @Bean
    fun personaUpdateQueue(): Queue =
        Queue(PERSONA_UPDATE_QUEUE_NAME, true)

    // =========================
    // Bindings
    // =========================

    @Bean
    fun aiRequestBinding(
        aiRequestQueue: Queue,
        aiExchange: DirectExchange
    ): Binding =
        BindingBuilder.bind(aiRequestQueue)
            .to(aiExchange)
            .with("ai.request")

    @Bean
    fun aiResponseBinding(
        aiResponseQueue: Queue,
        aiExchange: DirectExchange
    ): Binding =
        BindingBuilder.bind(aiResponseQueue)
            .to(aiExchange)
            .with("ai.response")

    // 🔥 FIX: korrekt routing key (inte "")
    @Bean
    fun sentimentBinding(
        sentimentQueue: Queue,
        storageExchange: DirectExchange
    ): Binding =
        BindingBuilder.bind(sentimentQueue)
            .to(storageExchange)
            .with("sentiment")

    // 🔥 FIX: matchar producer ("entity.detected")
    @Bean
    fun entityBinding(
        entityQueue: Queue,
        entityExchange: DirectExchange
    ): Binding =
        BindingBuilder.bind(entityQueue)
            .to(entityExchange)
            .with("entity.detected")

    // 🔥 Topic binding för persona
    @Bean
    fun personaUpdateBinding(
        personaUpdateQueue: Queue,
        personaExchange: TopicExchange
    ): Binding =
        BindingBuilder.bind(personaUpdateQueue)
            .to(personaExchange)
            .with("persona.update")

    // =========================
    // Infra (KRITISK för tester)
    // =========================

    // 🔥 Ser till att queues/exchanges deklareras vid startup
    @Bean
    fun rabbitAdmin(connectionFactory: ConnectionFactory): RabbitAdmin =
        RabbitAdmin(connectionFactory)

    // JSON converter
    @Bean
    fun messageConverter(objectMapper: ObjectMapper): MessageConverter =
        Jackson2JsonMessageConverter(objectMapper)
}