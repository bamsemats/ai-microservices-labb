package com.example.labb_microservices.content_aggregator.messaging

import com.example.labb_microservices.content_aggregator.model.ContentInjectionEvent
import com.example.labb_microservices.content_aggregator.model.EntityMessage
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.time.Duration

@Service
class EntityConsumer(
    private val rabbitTemplate: RabbitTemplate,
    private val redisTemplate: ReactiveRedisTemplate<String, Any>
) {

    private val logger = LoggerFactory.getLogger(EntityConsumer::class.java)

    @RabbitListener(queues = [RabbitMQConfig.ENTITY_QUEUE_NAME])
    fun processEntityDetection(entityMessage: EntityMessage): Mono<Void> {
        // Only process high-confidence semantic entities or legacy regex ones (conf = 1.0)
        if (entityMessage.confidence < 0.6) {
            logger.debug("Skipping low confidence entity: {} (conf: {})", entityMessage.entityValue, entityMessage.confidence)
            return Mono.empty()
        }

        logger.info("Processing detected entity: ${entityMessage.entityType} = ${entityMessage.entityValue} (conf: ${entityMessage.confidence})")
        
        return if (entityMessage.entityType == "GAME" || entityMessage.entityType == "STREAMER") {
            val typeKey = if (entityMessage.entityType == "STREAMER") "streamer" else "game"
            val cacheKey = "content:$typeKey:${entityMessage.entityValue.lowercase().replace(" ", "_")}"
            
            redisTemplate.opsForValue().get(cacheKey)
                .switchIfEmpty(
                    Mono.defer {
                        logger.info("Cache miss for ${entityMessage.entityValue}. Simulating API call...")
                        // Simulate fetching data from Twitch API
                        val twitchData = if (entityMessage.entityType == "STREAMER") {
                             mapOf(
                                "gameName" to "Just Chatting",
                                "streamer" to entityMessage.entityValue,
                                "viewers" to "${(5..20).random()}.${(0..9).random()}k",
                                "status" to "Live",
                                "thumbnail" to "https://placeholder.com/twitch-thumb.jpg",
                                "confidence" to entityMessage.confidence.toString()
                            )
                        } else {
                            mapOf(
                                "gameName" to entityMessage.entityValue,
                                "streamer" to listOf("Shroud", "Ninja", "Pokimane", "Lirik").random(),
                                "viewers" to "${(10..50).random()}.${(0..9).random()}k",
                                "status" to "Live",
                                "thumbnail" to "https://placeholder.com/twitch-thumb.jpg",
                                "confidence" to entityMessage.confidence.toString()
                            )
                        }
                        redisTemplate.opsForValue().set(cacheKey, twitchData, Duration.ofMinutes(10))
                            .thenReturn(twitchData)
                    }
                )
                .flatMap { data ->
                    @Suppress("UNCHECKED_CAST")
                    val event = ContentInjectionEvent(
                        contentType = "TWITCH_STREAM",
                        data = data as Map<String, String>
                    )
                    
                    logger.info("Publishing Content Injection Event for ${entityMessage.entityType}: ${entityMessage.entityValue}")
                    Mono.fromCallable {
                        rabbitTemplate.convertAndSend(
                            RabbitMQConfig.CONTENT_INJECTION_EXCHANGE_NAME,
                            "",
                            event
                        )
                    }.subscribeOn(Schedulers.boundedElastic())
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
                    }.subscribeOn(Schedulers.boundedElastic())
                }
                .then()
        } else if (entityMessage.entityType == "NEWS") {
            val cacheKey = "content:news:${entityMessage.entityValue.lowercase().replace(" ", "_")}"
            
            redisTemplate.opsForValue().get(cacheKey)
                .switchIfEmpty(
                    Mono.defer {
                        logger.info("Cache miss for ${entityMessage.entityValue}. Simulating News API call...")
                        val newsData = mapOf(
                            "title" to "Breaking: ${entityMessage.entityValue}",
                            "publisher" to "Global Tech News",
                            "summary" to "New developments have emerged regarding ${entityMessage.entityValue}. Experts suggest this could change the industry landscape significantly.",
                            "url" to "https://example.com/news/${entityMessage.entityValue.lowercase().replace(" ", "-")}",
                            "publishedAt" to "Just now"
                        )
                        redisTemplate.opsForValue().set(cacheKey, newsData, Duration.ofMinutes(10))
                            .thenReturn(newsData)
                    }
                )
                .flatMap { data ->
                    val event = ContentInjectionEvent(
                        contentType = "NEWS_ARTICLE",
                        data = data as Map<String, String>
                    )
                    
                    logger.info("Publishing Content Injection Event for news: ${entityMessage.entityValue}")
                    Mono.fromCallable { rabbitTemplate.convertAndSend(RabbitMQConfig.CONTENT_INJECTION_EXCHANGE_NAME, "", event) }.subscribeOn(Schedulers.boundedElastic())
                }
                .then()
        } else if (entityMessage.entityType == "SOCIAL") {
            val cacheKey = "content:social:${entityMessage.entityValue.lowercase().replace(" ", "_")}"
            
            redisTemplate.opsForValue().get(cacheKey)
                .switchIfEmpty(
                    Mono.defer {
                        logger.info("Cache miss for ${entityMessage.entityValue}. Simulating Social API call...")
                        val socialData = mapOf(
                            "author" to "@TechGuru",
                            "platform" to "X/Twitter",
                            "text" to "I can't believe the recent updates to ${entityMessage.entityValue}. Absolutely mind-blowing! 🤯 #tech #update",
                            "url" to "https://example.com/social/post/12345",
                            "likes" to "4.2k"
                        )
                        redisTemplate.opsForValue().set(cacheKey, socialData, Duration.ofMinutes(10))
                            .thenReturn(socialData)
                    }
                )
                .flatMap { data ->
                    val event = ContentInjectionEvent(
                        contentType = "SOCIAL_POST",
                        data = data as Map<String, String>
                    )
                    
                    logger.info("Publishing Content Injection Event for social: ${entityMessage.entityValue}")
                    Mono.fromCallable { rabbitTemplate.convertAndSend(RabbitMQConfig.CONTENT_INJECTION_EXCHANGE_NAME, "", event) }.subscribeOn(Schedulers.boundedElastic())
                }
                .then()
        } else if (entityMessage.entityType == "FORUM") {
            val cacheKey = "content:forum:${entityMessage.entityValue.lowercase().replace(" ", "_")}"
            
            redisTemplate.opsForValue().get(cacheKey)
                .switchIfEmpty(
                    Mono.defer {
                        logger.info("Cache miss for ${entityMessage.entityValue}. Simulating Forum API call...")
                        val forumData = mapOf(
                            "threadTitle" to "Discussion: ${entityMessage.entityValue} - Pros & Cons",
                            "forumName" to "DevTalk",
                            "author" to "code_ninja",
                            "excerpt" to "I've been trying out ${entityMessage.entityValue} lately and wanted to hear everyone's thoughts on the new features. Are they worth the hype?",
                            "url" to "https://example.com/forum/thread/9876",
                            "replies" to "128"
                        )
                        redisTemplate.opsForValue().set(cacheKey, forumData, Duration.ofMinutes(10))
                            .thenReturn(forumData)
                    }
                )
                .flatMap { data ->
                    val event = ContentInjectionEvent(
                        contentType = "FORUM_POST",
                        data = data as Map<String, String>
                    )
                    
                    logger.info("Publishing Content Injection Event for forum: ${entityMessage.entityValue}")
                    Mono.fromCallable { rabbitTemplate.convertAndSend(RabbitMQConfig.CONTENT_INJECTION_EXCHANGE_NAME, "", event) }.subscribeOn(Schedulers.boundedElastic())
                }
                .then()
        } else {
            Mono.empty()
        }
    }
}
