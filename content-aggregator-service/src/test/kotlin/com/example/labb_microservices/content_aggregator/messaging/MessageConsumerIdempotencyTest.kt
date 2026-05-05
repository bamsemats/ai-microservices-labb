package com.example.labb_microservices.content_aggregator.messaging

import com.example.labb_microservices.content_aggregator.model.Message
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import reactor.test.StepVerifier
import java.util.*

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = [
        "jwt.secret=a-very-long-and-secure-secret-key-that-is-at-least-256-bits",
        "encryption.secret=another-very-long-and-secure-secret-key-32-chars"
    ]
)
@Testcontainers
class MessageConsumerIdempotencyTest {

    companion object {
        @Container
        @ServiceConnection
        @JvmStatic
        val redis = GenericContainer("redis:7.2-alpine").withExposedPorts(6379)
        
        @Container
        @ServiceConnection
        @JvmStatic
        val rabbit = org.testcontainers.containers.RabbitMQContainer("rabbitmq:3.12-management")
    }

    @Autowired
    private lateinit var messageConsumer: MessageConsumer

    @Autowired
    private lateinit var redisTemplate: ReactiveRedisTemplate<String, Any>

    @Test
    fun `should only increment counters once for the same message ID`() {
        val messageId = UUID.randomUUID().toString()
        val channelId = "channel-123"
        val senderId = "user-456"
        
        val message = Message(
            id = messageId,
            content = "Hello world",
            senderId = senderId,
            receiverId = "all",
            channelId = channelId
        )

        val trendingKey = "trending:channels"
        val userStatsKey = "stats:user:$senderId:messages"

        val timeout = java.time.Duration.ofSeconds(5)

        // Process first time
        StepVerifier.create(messageConsumer.processDeliveredMessage(message))
            .verifyComplete()

        // Verify increments
        StepVerifier.create(redisTemplate.opsForZSet().score(trendingKey, channelId))
            .expectNext(1.0)
            .expectComplete()
            .verify(timeout)
            
        StepVerifier.create(redisTemplate.opsForValue().get(userStatsKey))
            .assertNext { value ->
                org.junit.jupiter.api.Assertions.assertEquals(1L, (value as Number).toLong())
            }
            .expectComplete()
            .verify(timeout)

        // Process second time (same messageId)
        StepVerifier.create(messageConsumer.processDeliveredMessage(message))
            .expectComplete()
            .verify(timeout)

        // Verify counters did NOT increment again
        StepVerifier.create(redisTemplate.opsForZSet().score(trendingKey, channelId))
            .expectNext(1.0)
            .expectComplete()
            .verify(timeout)
            
        StepVerifier.create(redisTemplate.opsForValue().get(userStatsKey))
            .assertNext { value ->
                org.junit.jupiter.api.Assertions.assertEquals(1L, (value as Number).toLong())
            }
            .expectComplete()
            .verify(timeout)
    }
}
