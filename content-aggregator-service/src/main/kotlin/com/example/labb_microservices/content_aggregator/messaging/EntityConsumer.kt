package com.example.labb_microservices.content_aggregator.messaging

import com.example.labb_microservices.content_aggregator.model.ContentInjectionEvent
import com.example.labb_microservices.content_aggregator.model.EntityMessage
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.time.Duration

@Service
class EntityConsumer(
    private val rabbitTemplate: RabbitTemplate,
    private val redisTemplate: ReactiveRedisTemplate<String, Any>
) {

    private val logger = LoggerFactory.getLogger(EntityConsumer::class.java)

    @RabbitListener(queues = [RabbitMQConfig.ENTITY_QUEUE_NAME])
    fun processEntityDetection(entityMessage: EntityMessage): Mono<Void> {
        logger.info("Processing detected entity: ${entityMessage.entityType} = ${entityMessage.entityValue}")
        
        return if (entityMessage.entityType == "GAME") {
            val cacheKey = "content:game:${entityMessage.entityValue.lowercase().replace(" ", "_")}"
            
            redisTemplate.opsForValue().get(cacheKey)
                .switchIfEmpty(
                    Mono.defer {
                        logger.info("Cache miss for ${entityMessage.entityValue}. Simulating API call...")
                        // Simulate fetching data from Twitch API
                        val twitchData = mapOf(
                            "gameName" to entityMessage.entityValue,
                            "streamer" to "NexusPrime",
                            "viewers" to "14.2k",
                            "status" to "Live",
                            "thumbnail" to "https://placeholder.com/twitch-thumb.jpg"
                        )
                        redisTemplate.opsForValue().set(cacheKey, twitchData, Duration.ofMinutes(10))
                            .thenReturn(twitchData)
                    }
                )
                .flatMap { data ->
                    val event = ContentInjectionEvent(
                        contentType = "TWITCH_STREAM",
                        data = data as Map<String, String>
                    )
                    
                    logger.info("Publishing Content Injection Event for game: ${entityMessage.entityValue}")
                    Mono.fromCallable {
                        rabbitTemplate.convertAndSend(
                            RabbitMQConfig.CONTENT_INJECTION_EXCHANGE_NAME,
                            "",
                            event
                        )
                    }
                }
                .then()
        } else if (entityMessage.entityType == "VIDEO") {
            val cacheKey = "content:video:${entityMessage.entityValue.lowercase().replace(" ", "_")}"
            
            redisTemplate.opsForValue().get(cacheKey)
                .switchIfEmpty(
                    Mono.defer {
                        logger.info("Cache miss for ${entityMessage.entityValue}. Simulating YouTube API call...")
                        val youtubeData = mapOf(
                            "title" to entityMessage.entityValue,
                            "channel" to "TechMaster",
                            "views" to "250k",
                            "publishedAt" to "2 days ago",
                            "duration" to "12:45",
                            "thumbnail" to "https://placeholder.com/youtube-thumb.jpg"
                        )
                        redisTemplate.opsForValue().set(cacheKey, youtubeData, Duration.ofMinutes(10))
                            .thenReturn(youtubeData)
                    }
                )
                .flatMap { data ->
                    val event = ContentInjectionEvent(
                        contentType = "YOUTUBE_VIDEO",
                        data = data as Map<String, String>
                    )
                    
                    logger.info("Publishing Content Injection Event for video: ${entityMessage.entityValue}")
                    Mono.fromCallable {
                        rabbitTemplate.convertAndSend(
                            RabbitMQConfig.CONTENT_INJECTION_EXCHANGE_NAME,
                            "",
                            event
                        )
                    }
                }
                .then()
        } else {
            Mono.empty()
        }
    }
}
