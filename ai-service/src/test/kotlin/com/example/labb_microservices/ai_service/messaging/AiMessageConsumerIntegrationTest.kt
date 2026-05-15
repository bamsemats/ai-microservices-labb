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
import org.mockito.Mockito.verify
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

import org.mockito.kotlin.any
import org.junit.jupiter.api.BeforeEach

@SpringBootTest(properties = ["openrouter.api.key=test-key"])
@Import(AiMessageConsumerIntegrationTest.TestConfig::class)
@org.springframework.test.annotation.DirtiesContext
class AiMessageConsumerIntegrationTest : BaseIntegrationTest() {

    @Autowired
    private lateinit var rabbitTemplate: RabbitTemplate

    @org.springframework.boot.test.context.TestConfiguration
    class TestConfig {
        @org.springframework.context.annotation.Bean
        fun testAdaptationQueue(): org.springframework.amqp.core.Queue {
            return org.springframework.amqp.core.Queue("test.adaptation.queue." + UUID.randomUUID().toString(), false, false, false)
        }

        @org.springframework.context.annotation.Bean
        fun testAdaptationBinding(testAdaptationQueue: org.springframework.amqp.core.Queue, adaptationExchange: org.springframework.amqp.core.FanoutExchange): org.springframework.amqp.core.Binding {
            return org.springframework.amqp.core.BindingBuilder.bind(testAdaptationQueue).to(adaptationExchange)
        }
    }

    @Autowired
    private lateinit var memoryFragmentRepository: com.example.labb_microservices.ai_service.repository.MemoryFragmentRepository

    @MockitoBean
    private lateinit var factExtractor: com.example.labb_microservices.ai_service.logic.FactExtractor

    @MockitoBean
    private lateinit var sentimentAnalyzer: com.example.labb_microservices.ai_service.logic.LlmSentimentAnalyzer

    @Autowired
    private lateinit var testAdaptationQueue: org.springframework.amqp.core.Queue

    @Autowired
    private lateinit var aiResponseQueue: org.springframework.amqp.core.Queue

    @Autowired
    private lateinit var rabbitAdmin: org.springframework.amqp.rabbit.core.RabbitAdmin

    @BeforeEach
    fun setUp() {
        // Ensure queue is declared before purge
        rabbitAdmin.declareQueue(testAdaptationQueue)
        
        // Drain adaptation queue using purge
        rabbitAdmin.purgeQueue(testAdaptationQueue.name)
        
        // Clear and seed repository
        memoryFragmentRepository.deleteAll().block()
        memoryFragmentRepository.save(
            com.example.labb_microservices.ai_service.model.MemoryFragment(
                userId = "user-test",
                category = com.example.labb_microservices.ai_service.model.MemoryCategory.INTEREST,
                value = "Test Context",
                confidence = 1.0,
                sourceMessageId = "test"
            )
        ).block()
        
        // Default stubs
        `when`(factExtractor.extractFacts(any())).thenReturn(Flux.empty())
        `when`(sentimentAnalyzer.analyzeSentiment(any())).thenReturn(Mono.empty())
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

        `when`(sentimentAnalyzer.analyzeSentiment(message.content)).thenReturn(Mono.just(
            AdaptationEvent(theme = "emergency", intensity = 0.9, glowIntensity = 0.9, color = "#f43f5e", blurAmount = 24.0, glassOpacity = 0.15)
        ))

        rabbitTemplate.convertAndSend(RabbitMQConfig.SENTIMENT_QUEUE_NAME, message)

        // Polling to handle multiple event types in fanout
        var event: AdaptationEvent? = null
        for (i in 1..20) {
            val received = rabbitTemplate.receiveAndConvert(testAdaptationQueue.name, 500)
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

        `when`(sentimentAnalyzer.analyzeSentiment(message.content)).thenReturn(Mono.just(
            AdaptationEvent(theme = "zen", intensity = 0.2, glowIntensity = 0.2, color = "#06b6d4", blurAmount = 8.0, glassOpacity = 0.02)
        ))

        rabbitTemplate.convertAndSend(RabbitMQConfig.SENTIMENT_QUEUE_NAME, message)

        var event: AdaptationEvent? = null
        for (i in 1..20) {
            val received = rabbitTemplate.receiveAndConvert(testAdaptationQueue.name, 500)
            if (received is AdaptationEvent) {
                event = received
                break
            }
        }

        assertNotNull(event, "Adaptation event should not be null")
        assertEquals("zen", event?.theme)
    }

    @Test
    fun `should process AI request and return response with simulated latency`() {
        val message = Message(
            id = UUID.randomUUID().toString(),
            senderId = "user-test",
            receiverId = "ai-bot",
            content = "What is the meaning of microservices?",
            authorType = AuthorType.USER,
            metadata = mapOf("X-Adapta-Test-Mode" to "true")
        )

        rabbitTemplate.convertAndSend(RabbitMQConfig.AI_EXCHANGE_NAME, "ai.request", message)

        // Give it more time (up to 15s)
        val response = rabbitTemplate.receiveAndConvert(RabbitMQConfig.AI_RESPONSE_QUEUE_NAME, 15000) as? Message

        assertNotNull(response, "AI response should not be null")
        assertEquals("ai-bot", response?.senderId)
        assertEquals("user-test", response?.receiverId)
        assertEquals(AuthorType.BOT, response?.authorType)
        assertEquals("Deterministic mock response. Context found: [Test Context]", response?.content)
    }
}
