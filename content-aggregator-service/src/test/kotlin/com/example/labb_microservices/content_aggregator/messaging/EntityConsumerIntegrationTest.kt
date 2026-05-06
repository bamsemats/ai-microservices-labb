package com.example.labb_microservices.content_aggregator.messaging

import com.example.common.test.BaseIntegrationTest
import com.example.labb_microservices.content_aggregator.model.ContentInjectionEvent
import com.example.labb_microservices.content_aggregator.model.EntityMessage
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.redis.core.ReactiveRedisTemplate
import reactor.test.StepVerifier
import java.time.Duration
import java.util.*

@SpringBootTest(properties = [
    "jwt.secret=a-very-long-and-secure-secret-key-that-is-at-least-256-bits",
    "encryption.secret=another-very-long-and-secure-secret-key-32-chars"
])
class EntityConsumerIntegrationTest : BaseIntegrationTest() {

    @Autowired
    private lateinit var rabbitTemplate: RabbitTemplate

    @Autowired
    private lateinit var redisTemplate: ReactiveRedisTemplate<String, Any>

    @org.springframework.boot.test.context.TestConfiguration
    class TestConfig {
        @org.springframework.context.annotation.Bean
        fun testContentQueue(): org.springframework.amqp.core.Queue {
            return org.springframework.amqp.core.Queue("test.content.queue", false)
        }

        @org.springframework.context.annotation.Bean
        fun testContentBinding(testContentQueue: org.springframework.amqp.core.Queue, contentInjectionExchange: org.springframework.amqp.core.FanoutExchange): org.springframework.amqp.core.Binding {
            return org.springframework.amqp.core.BindingBuilder.bind(testContentQueue).to(contentInjectionExchange)
        }
    }

    @Test
    fun `should cache content and publish injection event when game is detected`() {
        val gameName = "Elden Ring"
        val entityMessage = EntityMessage(
            entityType = "GAME",
            entityValue = gameName,
            originalMessageId = UUID.randomUUID().toString(),
            senderId = "user-1"
        )

        val cacheKey = "content:game:elden_ring"

        // Ensure cache is empty
        redisTemplate.delete(cacheKey).block()

        // Send message to queue
        rabbitTemplate.convertAndSend(RabbitMQConfig.ENTITY_EXCHANGE_NAME, "entity.detected", entityMessage)

        // Verify event is published
        val event = rabbitTemplate.receiveAndConvert("test.content.queue", 5000) as? ContentInjectionEvent
        assertNotNull(event, "Content injection event should not be null")
        assertEquals("TWITCH_STREAM", event?.contentType)
        assertEquals(gameName, event?.data?.get("gameName"))

        // Verify async Redis write and verify content
        StepVerifier.create(redisTemplate.opsForValue().get(cacheKey))
            .assertNext { cachedData ->
                val data = cachedData as? Map<*, *>
                assertNotNull(data, "Cached data should be a Map but was ${cachedData?.let { it::class.simpleName } ?: "null"}")
                assertEquals(gameName, data!!["gameName"])
                assertEquals("NexusPrime", data["streamer"])
            }
            .expectComplete()
            .verify(Duration.ofSeconds(5))
    }
}
