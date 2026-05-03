package com.example.labb_microservices.ai_service.messaging

import com.example.labb_microservices.ai_service.model.AdaptationEvent
import com.example.labb_microservices.ai_service.model.AuthorType
import com.example.labb_microservices.ai_service.model.Message
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.testcontainers.containers.RabbitMQContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.*
import java.util.concurrent.TimeUnit

@SpringBootTest
@Testcontainers
class AiMessageConsumerIntegrationTest {

    companion object {
        @Container
        @ServiceConnection
        val rabbit = RabbitMQContainer("rabbitmq:3.12-management")
    }

    @Autowired
    private lateinit var rabbitTemplate: RabbitTemplate

    @org.springframework.boot.test.context.TestConfiguration
    class TestConfig {
        @org.springframework.context.annotation.Bean
        fun testAdaptationQueue(): org.springframework.amqp.core.Queue {
            return org.springframework.amqp.core.Queue("test.adaptation.queue", false)
        }

        @org.springframework.context.annotation.Bean
        fun testAdaptationBinding(testAdaptationQueue: org.springframework.amqp.core.Queue, adaptationExchange: org.springframework.amqp.core.FanoutExchange): org.springframework.amqp.core.Binding {
            return org.springframework.amqp.core.BindingBuilder.bind(testAdaptationQueue).to(adaptationExchange)
        }
    }

    @Test
    fun `should publish adaptation event when urgent sentiment is detected`() {
        val message = Message(
            id = UUID.randomUUID().toString(),
            senderId = "user-1",
            receiverId = "all",
            content = "This is URGENT! I need HELP!",
            authorType = AuthorType.USER
        )

        rabbitTemplate.convertAndSend(RabbitMQConfig.SENTIMENT_QUEUE_NAME, message)

        // Give it a moment to process
        val event = rabbitTemplate.receiveAndConvert("test.adaptation.queue", 5000) as? AdaptationEvent

        assertNotNull(event, "Adaptation event should not be null")
        assertEquals("emergency", event?.theme)
        assertEquals(0.9, event?.intensity)
    }

    @Test
    fun `should publish adaptation event when zen sentiment is detected`() {
        val message = Message(
            id = UUID.randomUUID().toString(),
            senderId = "user-2",
            receiverId = "all",
            content = "I am feeling very calm and relaxed. Time to sleep.",
            authorType = AuthorType.USER
        )

        rabbitTemplate.convertAndSend(RabbitMQConfig.SENTIMENT_QUEUE_NAME, message)

        val event = rabbitTemplate.receiveAndConvert("test.adaptation.queue", 5000) as? AdaptationEvent

        assertNotNull(event, "Adaptation event should not be null")
        assertEquals("zen", event?.theme)
        assertEquals(0.2, event?.intensity)
    }
}
