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

        logger.info("Processing detected entity: ${entityMessage.entityType} = ${entityMessage.entityValue} (conf: ${entityMessage.confidence}) in channel ${entityMessage.channelId}")
        
        // Topic Reasoning & Deduplication
        val dedupKey = "dedup:injection:${entityMessage.channelId}:${entityMessage.entityValue.lowercase().replace(" ", "_")}"
        
        return redisTemplate.opsForValue().setIfAbsent(dedupKey, "true", Duration.ofMinutes(5))
            .flatMap { isNew ->
                if (isNew == false) {
                    logger.info("Deduplicating injection for ${entityMessage.entityValue} in channel ${entityMessage.channelId}")
                    return@flatMap Mono.empty<Void>()
                }
                
                if (entityMessage.entityType == "GAME" || entityMessage.entityType == "STREAMER") {
                    val typeKey = if (entityMessage.entityType == "STREAMER") "streamer" else "game"
                    val cacheKey = "content:$typeKey:${entityMessage.entityValue.lowercase().replace(" ", "_")}"
                    
                    redisTemplate.opsForValue().get(cacheKey)
                        .switchIfEmpty(
                            Mono.defer {
                                logger.info("Cache miss for ${entityMessage.entityValue}. Simulating API call...")
                                // Simulate fetching data from Twitch API
                                val twitchData = if (entityMessage.entityType == "STREAMER") {
                                     mapOf(
                                        "gameName" to listOf("Just Chatting", "VALORANT", "League of Legends", "Minecraft", "Counter-Strike 2").random(),
                                        "streamer" to entityMessage.entityValue,
                                        "viewers" to "${(5..40).random()}.${(0..9).random()}k",
                                        "status" to "Live",
                                        "thumbnail" to "https://placeholder.com/twitch-thumb.jpg",
                                        "confidence" to entityMessage.confidence.toString()
                                    )
                                } else {
                                    mapOf(
                                        "gameName" to entityMessage.entityValue,
                                        "streamer" to listOf("Shroud", "Ninja", "Pokimane", "Lirik", "KaiCenat", "xQc").random(),
                                        "viewers" to "${(10..80).random()}.${(0..9).random()}k",
                                        "status" to "Live",
                                        "thumbnail" to "https://placeholder.com/twitch-thumb.jpg",
                                        "confidence" to entityMessage.confidence.toString()
                                    )
                                }
                                redisTemplate.opsForValue().set(cacheKey, twitchData, Duration.ofMinutes(15))
                                    .thenReturn(twitchData)
                            }
                        )
                        .flatMap { data ->
                            @Suppress("UNCHECKED_CAST")
                            val event = ContentInjectionEvent(
                                contentType = "TWITCH_STREAM",
                                channelId = entityMessage.channelId,
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
                } else if (entityMessage.entityType == "VIDEO") {
                    val cacheKey = "content:video:${entityMessage.entityValue.lowercase().replace(" ", "_")}"
                    
                    redisTemplate.opsForValue().get(cacheKey)
                        .switchIfEmpty(
                            Mono.defer {
                                logger.info("Cache miss for ${entityMessage.entityValue}. Simulating YouTube API call...")
                                val youtubeData = mapOf(
                                    "title" to entityMessage.entityValue,
                                    "channel" to if (entityMessage.entityValue.length % 2 == 0) "ContentHub" else "Official Channel",
                                    "views" to "${(100..999).random()}k",
                                    "publishedAt" to "${(1..7).random()} days ago",
                                    "duration" to "${(5..20).random()}:${(10..59).random()}",
                                    "thumbnail" to "https://placeholder.com/youtube-thumb.jpg"
                                )
                                redisTemplate.opsForValue().set(cacheKey, youtubeData, Duration.ofMinutes(15))
                                    .thenReturn(youtubeData)
                            }
                        )
                        .flatMap { data ->
                            @Suppress("UNCHECKED_CAST")
                            val event = ContentInjectionEvent(
                                contentType = "YOUTUBE_VIDEO",
                                channelId = entityMessage.channelId,
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
                } else if (entityMessage.entityType == "NEWS") {
                    val cacheKey = "content:news:${entityMessage.entityValue.lowercase().replace(" ", "_")}"
                    
                    redisTemplate.opsForValue().get(cacheKey)
                        .switchIfEmpty(
                            Mono.defer {
                                logger.info("Cache miss for ${entityMessage.entityValue}. Simulating News API call...")
                                val newsData = mapOf(
                                    "title" to "Updates regarding ${entityMessage.entityValue}",
                                    "publisher" to listOf("Global Tech News", "Daily Beacon", "Sector Weekly").random(),
                                    "summary" to "New reports have surfaced regarding ${entityMessage.entityValue}. Analysis suggests a significant impact on local frequencies and data stability.",
                                    "url" to "https://example.com/news/${entityMessage.entityValue.lowercase().replace(" ", "-")}",
                                    "publishedAt" to "Recently"
                                )
                                redisTemplate.opsForValue().set(cacheKey, newsData, Duration.ofMinutes(15))
                                    .thenReturn(newsData)
                            }
                        )
                        .flatMap { data ->
                            @Suppress("UNCHECKED_CAST")
                            val event = ContentInjectionEvent(
                                contentType = "NEWS_ARTICLE",
                                channelId = entityMessage.channelId,
                                data = data as Map<String, String>
                            )
                            
                            logger.info("Publishing Content Injection Event for news: ${entityMessage.entityValue}")
                            Mono.fromCallable { rabbitTemplate.convertAndSend(RabbitMQConfig.CONTENT_INJECTION_EXCHANGE_NAME, "", event) }.subscribeOn(Schedulers.boundedElastic())
                        }
                } else if (entityMessage.entityType == "SOCIAL") {
                    val cacheKey = "content:social:${entityMessage.entityValue.lowercase().replace(" ", "_")}"
                    
                    redisTemplate.opsForValue().get(cacheKey)
                        .switchIfEmpty(
                            Mono.defer {
                                logger.info("Cache miss for ${entityMessage.entityValue}. Simulating Social API call...")
                                val socialData = mapOf(
                                    "author" to "@FrequencyObserver",
                                    "platform" to "AdaptaSocial",
                                    "text" to "Did anyone else notice the resonance patterns with ${entityMessage.entityValue}? Interesting shift in the network today. #adapta #frequency",
                                    "url" to "https://example.com/social/post/${(10000..99999).random()}",
                                    "likes" to "${(1..9).random()}.${(0..9).random()}k"
                                )
                                redisTemplate.opsForValue().set(cacheKey, socialData, Duration.ofMinutes(15))
                                    .thenReturn(socialData)
                            }
                        )
                        .flatMap { data ->
                            @Suppress("UNCHECKED_CAST")
                            val event = ContentInjectionEvent(
                                contentType = "SOCIAL_POST",
                                channelId = entityMessage.channelId,
                                data = data as Map<String, String>
                            )
                            
                            logger.info("Publishing Content Injection Event for social: ${entityMessage.entityValue}")
                            Mono.fromCallable { rabbitTemplate.convertAndSend(RabbitMQConfig.CONTENT_INJECTION_EXCHANGE_NAME, "", event) }.subscribeOn(Schedulers.boundedElastic())
                        }
                } else if (entityMessage.entityType == "FORUM") {
                    val cacheKey = "content:forum:${entityMessage.entityValue.lowercase().replace(" ", "_")}"
                    
                    redisTemplate.opsForValue().get(cacheKey)
                        .switchIfEmpty(
                            Mono.defer {
                                logger.info("Cache miss for ${entityMessage.entityValue}. Simulating Forum API call...")
                                val forumData = mapOf(
                                    "threadTitle" to "Community thoughts on ${entityMessage.entityValue}",
                                    "forumName" to "Nexus Hub",
                                    "author" to "echo_protocol",
                                    "excerpt" to "I've been analyzing the throughput of ${entityMessage.entityValue} and the results are quite unexpected compared to previous benchmarks.",
                                    "url" to "https://example.com/forum/thread/${(1000..9999).random()}",
                                    "replies" to "${(20..250).random()}"
                                )
                                redisTemplate.opsForValue().set(cacheKey, forumData, Duration.ofMinutes(15))
                                    .thenReturn(forumData)
                            }
                        )
                        .flatMap { data ->
                            @Suppress("UNCHECKED_CAST")
                            val event = ContentInjectionEvent(
                                contentType = "FORUM_POST",
                                channelId = entityMessage.channelId,
                                data = data as Map<String, String>
                            )
                            
                            logger.info("Publishing Content Injection Event for forum: ${entityMessage.entityValue}")
                            Mono.fromCallable { rabbitTemplate.convertAndSend(RabbitMQConfig.CONTENT_INJECTION_EXCHANGE_NAME, "", event) }.subscribeOn(Schedulers.boundedElastic())
                        }
                } else {
                    Mono.empty<Void>()
                }
            }
            .then()
    }
}
