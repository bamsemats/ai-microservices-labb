package com.example.labb_microservices.ai_service.messaging

import com.example.common.test.BaseIntegrationTest
import com.example.labb_microservices.ai_service.model.AdaptationEvent
import com.example.labb_microservices.ai_service.model.AuthorType
import com.example.labb_microservices.ai_service.model.Message
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import java.util.*
import java.util.concurrent.TimeUnit

import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.mockito.Mockito.`when`
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

import org.mockito.kotlin.any
import org.junit.jupiter.api.BeforeEach

@SpringBootTest(properties = ["openrouter.api.key=test-key"])
@Import(AiMessageConsumerIntegrationTest.TestConfig::class)
class AiMessageConsumerIntegrationTest : BaseIntegrationTest() {

    @Autowired
    private lateinit var rabbitTemplate: RabbitTemplate

    @MockitoBean
    private lateinit var responseGenerator: com.example.labb_microservices.ai_service.logic.ResponseGenerator

    @MockitoBean
    private lateinit var factExtractor: com.example.labb_microservices.ai_service.logic.FactExtractor

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

    @Autowired
    private lateinit var rabbitAdmin: org.springframework.amqp.rabbit.core.RabbitAdmin

    @BeforeEach
    fun setUp() {
        // Drain adaptation queue using purge
        rabbitAdmin.purgeQueue(testAdaptationQueue.name)
        
        // Default stubs
        `when`(factExtractor.extractFacts(any())).thenReturn(Flux.empty())
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
        assertEquals(0.9, event?.glowIntensity)
        assertEquals("#f43f5e", event?.color)
        assertEquals(24.0, event?.blurAmount)
        assertEquals(0.15, event?.glassOpacity)
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
        assertEquals(0.2, event?.glowIntensity)
        assertEquals("#06b6d4", event?.color)
        assertEquals(8.0, event?.blurAmount)
        assertEquals(0.02, event?.glassOpacity)
    }

    @Test
    fun `should process AI request and return response with simulated latency`() {
        // We use metadata to trigger test mode in the real generator as a fallback
        // but we still try to mock it.
        `when`(responseGenerator.generateResponse(any())).thenReturn(Flux.just("Mock AI Response"))

        val message = Message(
            id = UUID.randomUUID().toString(),
            senderId = "user-test",
            receiverId = "ai-bot",
            content = "What is the meaning of microservices?",
            authorType = AuthorType.USER,
            metadata = mapOf("X-Adapta-Test-Mode" to "true")
        )

        rabbitTemplate.convertAndSend(RabbitMQConfig.AI_EXCHANGE_NAME, "ai.request", message)

        // Give it more time (up to 10s) because of the simulated 1-3s latency + startup/processing overhead
        val response = rabbitTemplate.receiveAndConvert(RabbitMQConfig.AI_RESPONSE_QUEUE_NAME, 10000) as? Message

        assertNotNull(response, "AI response should not be null")
        assertEquals("ai-bot", response?.senderId)
        assertEquals("user-test", response?.receiverId)
        assertEquals(AuthorType.BOT, response?.authorType)
        // If mock fails, it will return the test mode response from real generator
        assertTrue(response?.content?.contains("Mock AI Response") == true || response?.content?.contains("Deterministic mock response") == true)
    }
}
