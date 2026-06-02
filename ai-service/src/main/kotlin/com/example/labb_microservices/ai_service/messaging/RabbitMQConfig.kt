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

        const val PERSONA_EXCHANGE_NAME = "chat.persona.exchange"

        const val DLX_NAME = "ai.dlx"
        const val DLQ_NAME = "ai.dlq"
        const val DLX_ROUTING_KEY = "dead-letter"
    }

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

    @Bean
    fun aiDeadLetterExchange(): DirectExchange =
        DirectExchange(DLX_NAME)

    @Bean
    fun aiDeadLetterQueue(): Queue =
        Queue(DLQ_NAME, true)

    @Bean
    fun aiDeadLetterBinding(aiDeadLetterQueue: Queue, aiDeadLetterExchange: DirectExchange): Binding =
        BindingBuilder.bind(aiDeadLetterQueue)
            .to(aiDeadLetterExchange)
            .with(DLX_ROUTING_KEY)

    @Bean
    fun aiRequestQueue(): Queue =
        QueueBuilder.durable(AI_REQUEST_QUEUE_NAME)
            .withArgument("x-dead-letter-exchange", DLX_NAME)
            .withArgument("x-dead-letter-routing-key", DLX_ROUTING_KEY)
            .build()

    @Bean
    fun aiResponseQueue(): Queue =
        QueueBuilder.durable(AI_RESPONSE_QUEUE_NAME)
            .withArgument("x-dead-letter-exchange", DLX_NAME)
            .withArgument("x-dead-letter-routing-key", DLX_ROUTING_KEY)
            .build()

    @Bean
    fun sentimentQueue(): Queue =
        QueueBuilder.durable(SENTIMENT_QUEUE_NAME)
            .withArgument("x-dead-letter-exchange", DLX_NAME)
            .withArgument("x-dead-letter-routing-key", DLX_ROUTING_KEY)
            .build()

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

    @Bean
    fun sentimentBinding(
        sentimentQueue: Queue,
        storageExchange: DirectExchange
    ): Binding =
        BindingBuilder.bind(sentimentQueue)
            .to(storageExchange)
            .with("message-published")

    @Bean
    fun rabbitAdmin(connectionFactory: ConnectionFactory): RabbitAdmin =
        RabbitAdmin(connectionFactory)

    @Bean
    fun messageConverter(objectMapper: ObjectMapper): MessageConverter =
        Jackson2JsonMessageConverter(objectMapper)
}