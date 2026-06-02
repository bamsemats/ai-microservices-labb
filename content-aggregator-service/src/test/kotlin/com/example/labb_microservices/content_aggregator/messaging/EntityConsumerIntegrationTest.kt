package com.example.labb_microservices.content_aggregator.messaging

import com.example.labb_microservices.common.test.BaseIntegrationTest
import com.example.labb_microservices.content_aggregator.model.ContentInjectionEvent
import com.example.labb_microservices.content_aggregator.model.EntityMessage
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.amqp.core.Binding
import org.springframework.amqp.core.BindingBuilder
import org.springframework.amqp.core.FanoutExchange
import org.springframework.amqp.core.Queue
import org.springframework.amqp.rabbit.core.RabbitAdmin
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.data.redis.core.ReactiveRedisTemplate
import reactor.test.StepVerifier
import java.time.Duration
import java.util.*

@SpringBootTest
class EntityConsumerIntegrationTest : BaseIntegrationTest() {

    @Autowired
    private lateinit var rabbitTemplate: RabbitTemplate

    @Autowired
    private lateinit var redisTemplate: ReactiveRedisTemplate<String, Any>

    @TestConfiguration
    class TestConfig {
        @Bean
        fun rabbitAdmin(connectionFactory: org.springframework.amqp.rabbit.connection.ConnectionFactory): RabbitAdmin {
            return RabbitAdmin(connectionFactory)
        }

        @Bean
        fun testContentQueue(): Queue {
            return Queue("test.content.queue", false)
        }

        @Bean
        fun testContentBinding(testContentQueue: Queue, contentInjectionExchange: FanoutExchange): Binding {
            return BindingBuilder.bind(testContentQueue).to(contentInjectionExchange)
        }
    }

    @Autowired
    private lateinit var rabbitAdmin: RabbitAdmin

    @Test
    fun `should cache content and publish injection event when game is detected`() {
        val gameName = "Elden Ring"
        val channelId = "test-channel"
        val entityMessage = EntityMessage(
            entityType = "GAME",
            entityValue = gameName,
            originalMessageId = UUID.randomUUID().toString(),
            channelId = channelId,
            senderId = "user-1"
        )

        val cacheKey = "content:game:elden_ring"
        val dedupKey = "dedup:injection:$channelId:elden_ring"

        // Drain queue first using purge
        rabbitAdmin.purgeQueue("test.content.queue")

        // Ensure cache and dedup are empty
        redisTemplate.delete(cacheKey).block()
        redisTemplate.delete(dedupKey).block()

        // Send message to queue
        rabbitTemplate.convertAndSend(RabbitMQConfig.ENTITY_EXCHANGE_NAME, "entity.detected", entityMessage)

        // Verify event is published
        val event = rabbitTemplate.receiveAndConvert("test.content.queue", 10000) as? ContentInjectionEvent
        assertNotNull(event, "received ContentInjectionEvent from test.content.queue within timeout")
        assertEquals("TWITCH_STREAM", event?.contentType)
        assertEquals(channelId, event?.channelId)
        assertEquals(gameName, event?.data?.get("gameName"))

        // Verify async Redis write and verify content
        StepVerifier.create(redisTemplate.opsForValue().get(cacheKey))
            .assertNext { cachedData ->
                val data = cachedData as? Map<*, *>
                assertNotNull(data, "Cached data should be a Map but was ${cachedData?.let { it::class.simpleName } ?: "null"}")
                assertEquals(gameName, data!!["gameName"])
                assertTrue(data["streamer"].toString().isNotBlank())
            }
            .expectComplete()
            .verify(Duration.ofSeconds(5))

        // Verify deduplication: send again and expect nothing in queue
        rabbitAdmin.purgeQueue("test.content.queue")
        rabbitTemplate.convertAndSend(RabbitMQConfig.ENTITY_EXCHANGE_NAME, "entity.detected", entityMessage)
        val secondEvent = rabbitTemplate.receiveAndConvert("test.content.queue", 2000)
        assertNull(secondEvent, "Second event should be deduplicated")
    }
}
