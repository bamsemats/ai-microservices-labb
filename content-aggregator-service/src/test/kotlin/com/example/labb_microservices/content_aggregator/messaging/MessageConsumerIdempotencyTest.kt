package com.example.labb_microservices.content_aggregator.messaging

import com.example.common.test.BaseIntegrationTest
import com.example.labb_microservices.content_aggregator.model.Message
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.redis.core.ReactiveRedisTemplate
import reactor.test.StepVerifier
import java.util.*

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = [
        "jwt.secret=a-very-long-and-secure-secret-key-that-is-at-least-256-bits",
        "encryption.secret=another-very-long-and-secure-secret-key-32-chars"
    ]
)
class MessageConsumerIdempotencyTest : BaseIntegrationTest() {

    @Autowired
    private lateinit var messageConsumer: MessageConsumer

    @Autowired
    private lateinit var redisTemplate: ReactiveRedisTemplate<String, Any>

    @Test
    fun `should only increment counters once for the same message ID`() {
        val messageId = UUID.randomUUID().toString()
        val uniqueId = UUID.randomUUID().toString().take(8)
        val channelId = "channel-$uniqueId"
        val senderId = "user-$uniqueId"
        
        // Clear Redis state for isolation
        redisTemplate.execute { it.serverCommands().flushAll() }.blockFirst()
        
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
