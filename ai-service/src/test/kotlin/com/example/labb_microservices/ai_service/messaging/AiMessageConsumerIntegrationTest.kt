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
import org.springframework.context.annotation.Import
import java.util.*
import java.util.concurrent.TimeUnit

@SpringBootTest
@Testcontainers
@Import(AiMessageConsumerIntegrationTest.TestConfig::class)
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
            return org.springframework.amqp.core.Queue("test.adaptation.queue." + UUID.randomUUID().toString(), true, false, false)
        }

        @org.springframework.context.annotation.Bean
        fun testAdaptationBinding(testAdaptationQueue: org.springframework.amqp.core.Queue, adaptationExchange: org.springframework.amqp.core.FanoutExchange): org.springframework.amqp.core.Binding {
            return org.springframework.amqp.core.BindingBuilder.bind(testAdaptationQueue).to(adaptationExchange)
        }
    }

    @Autowired
    private lateinit var testAdaptationQueue: org.springframework.amqp.core.Queue

    @Autowired
    private lateinit var aiResponseQueue: org.springframework.amqp.core.Queue

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
        val event = rabbitTemplate.receiveAndConvert(testAdaptationQueue.name, 5000) as? AdaptationEvent

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

        val event = rabbitTemplate.receiveAndConvert(testAdaptationQueue.name, 5000) as? AdaptationEvent

        assertNotNull(event, "Adaptation event should not be null")
        assertEquals("zen", event?.theme)
        assertEquals(0.2, event?.intensity)
    }

    @Test
    fun `should process AI request and return response with simulated latency`() {
        val message = Message(
            id = UUID.randomUUID().toString(),
            senderId = "user-test",
            receiverId = "ai-bot",
            content = "What is the meaning of microservices?",
            authorType = AuthorType.USER
        )

        rabbitTemplate.convertAndSend(RabbitMQConfig.AI_EXCHANGE_NAME, "ai.request", message)

        // Give it more time (up to 10s) because of the simulated 1-3s latency + startup/processing overhead
        val response = rabbitTemplate.receiveAndConvert(RabbitMQConfig.AI_RESPONSE_QUEUE_NAME, 10000) as? Message

        assertNotNull(response, "AI response should not be null")
        assertEquals("ai-bot", response?.senderId)
        assertEquals("user-test", response?.receiverId)
        assertEquals(AuthorType.BOT, response?.authorType)
        assertNotNull(response?.content)
    }
}
