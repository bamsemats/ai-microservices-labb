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

import org.springframework.boot.test.mock.mockito.MockBean
import org.mockito.Mockito.`when`
import org.mockito.ArgumentMatchers.any
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@SpringBootTest(properties = ["openrouter.api.key=test-key"])
@Testcontainers
@Import(AiMessageConsumerIntegrationTest.TestConfig::class)
class AiMessageConsumerIntegrationTest {

    companion object {
        @Container
        @ServiceConnection
        val rabbit = RabbitMQContainer("rabbitmq:3.12-management")

        @Container
        @ServiceConnection
        val mongo = org.testcontainers.containers.MongoDBContainer("mongo:7.0")
    }

    @Autowired
    private lateinit var rabbitTemplate: RabbitTemplate

    @MockBean
    private lateinit var responseGenerator: com.example.labb_microservices.ai_service.logic.ResponseGenerator

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

    private fun <T> anySafe(type: Class<T>): T {
        any(type)
        @Suppress("UNCHECKED_CAST")
        return null as T
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

        // Polling to handle multiple event types in fanout
        var event: AdaptationEvent? = null
        for (i in 1..10) {
            val received = rabbitTemplate.receiveAndConvert(testAdaptationQueue.name, 1000)
            if (received is AdaptationEvent) {
                event = received
                break
            }
        }

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

        var event: AdaptationEvent? = null
        for (i in 1..10) {
            val received = rabbitTemplate.receiveAndConvert(testAdaptationQueue.name, 1000)
            if (received is AdaptationEvent) {
                event = received
                break
            }
        }

        assertNotNull(event, "Adaptation event should not be null")
        assertEquals("zen", event?.theme)
        assertEquals(0.2, event?.intensity)
    }

    @Test
    fun `should process AI request and return response with simulated latency`() {
        `when`(responseGenerator.generateResponse(anySafe(Message::class.java))).thenReturn(Flux.just("Mock AI Response"))

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
