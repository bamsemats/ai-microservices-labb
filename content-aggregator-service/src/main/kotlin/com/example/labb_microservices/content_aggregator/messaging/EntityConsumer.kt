package com.example.labb_microservices.content_aggregator.messaging

import com.example.labb_microservices.content_aggregator.model.ContentInjectionEvent
import com.example.labb_microservices.content_aggregator.model.EntityMessage
import com.example.labb_microservices.content_aggregator.service.ContentSource
import com.example.labb_microservices.content_aggregator.service.LibrarianService
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
    private val redisTemplate: ReactiveRedisTemplate<String, Any>,
    private val librarianService: LibrarianService
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
        
        // Topic Reasoning
        val reasoning = librarianService.reasonAboutEntity(entityMessage.entityType, entityMessage.entityValue)
        
        // Deduplication
        val normalizedValue = entityMessage.entityValue.lowercase().replace(" ", "_")
        val dedupKey = "dedup:injection:${entityMessage.channelId}:${reasoning.primarySource.name.lowercase()}:$normalizedValue"
        
        return redisTemplate.opsForValue().setIfAbsent(dedupKey, "true", Duration.ofMinutes(5))
            .flatMap { isNew ->
                if (isNew == false) {
                    logger.info("Deduplicating injection for ${entityMessage.entityValue} in channel ${entityMessage.channelId}")
                    return@flatMap Mono.empty<Void>()
                }
                
                val sourceAction = when (reasoning.primarySource) {
                    ContentSource.TWITCH -> handleTwitchSource(entityMessage)
                    ContentSource.YOUTUBE -> handleYoutubeSource(entityMessage)
                    ContentSource.NEWS -> handleNewsSource(entityMessage)
                    ContentSource.SOCIAL -> handleSocialSource(entityMessage)
                    ContentSource.FORUM -> handleForumSource(entityMessage)
                }

                sourceAction.onErrorResume { error ->
                    logger.error("Failed to process injection for ${entityMessage.entityValue}: ${error.message}")
                    redisTemplate.delete(dedupKey).then(Mono.empty())
                }
            }
            .then()
    }

    private fun handleTwitchSource(entity: EntityMessage): Mono<Void> {
        val cacheKey = "content:twitch:${entity.entityValue.lowercase().replace(" ", "_")}"
        return redisTemplate.opsForValue().get(cacheKey)
            .switchIfEmpty(
                Mono.defer {
                    logger.info("Cache miss for Twitch entity ${entity.entityValue}. Simulating API call...")
                    val twitchData = mapOf(
                        "gameName" to listOf("Just Chatting", "VALORANT", "League of Legends", "Minecraft", "Counter-Strike 2").random(),
                        "streamer" to entity.entityValue,
                        "viewers" to "${(5..40).random()}.${(0..9).random()}k",
                        "status" to "Live",
                        "thumbnail" to "https://placeholder.com/twitch-thumb.jpg",
                        "confidence" to entity.confidence.toString()
                    )
                    redisTemplate.opsForValue().set(cacheKey, twitchData, Duration.ofMinutes(15)).thenReturn(twitchData)
                }
            )
            .flatMap { data ->
                @Suppress("UNCHECKED_CAST")
                val event = ContentInjectionEvent(
                    contentType = "TWITCH_STREAM",
                    channelId = entity.channelId,
                    data = data as Map<String, String>
                )
                dispatchEvent(event)
            }
    }

    private fun handleYoutubeSource(entity: EntityMessage): Mono<Void> {
        val cacheKey = "content:youtube:${entity.entityValue.lowercase().replace(" ", "_")}"
        return redisTemplate.opsForValue().get(cacheKey)
            .switchIfEmpty(
                Mono.defer {
                    logger.info("Cache miss for YouTube entity ${entity.entityValue}. Simulating API call...")
                    val youtubeData = mapOf(
                        "title" to "Best of ${entity.entityValue} Highlights",
                        "channel" to entity.entityValue,
                        "views" to "${(100..999).random()}k",
                        "publishedAt" to "${(1..7).random()} days ago",
                        "duration" to "${(5..20).random()}:${(10..59).random()}",
                        "thumbnail" to "https://placeholder.com/youtube-thumb.jpg"
                    )
                    redisTemplate.opsForValue().set(cacheKey, youtubeData, Duration.ofMinutes(15)).thenReturn(youtubeData)
                }
            )
            .flatMap { data ->
                @Suppress("UNCHECKED_CAST")
                val event = ContentInjectionEvent(
                    contentType = "YOUTUBE_VIDEO",
                    channelId = entity.channelId,
                    data = data as Map<String, String>
                )
                dispatchEvent(event)
            }
    }

    private fun handleNewsSource(entity: EntityMessage): Mono<Void> {
        val cacheKey = "content:news:${entity.entityValue.lowercase().replace(" ", "_")}"
        return redisTemplate.opsForValue().get(cacheKey)
            .switchIfEmpty(
                Mono.defer {
                    val newsData = mapOf(
                        "title" to "Updates regarding ${entity.entityValue}",
                        "publisher" to listOf("Global Tech News", "Daily Beacon", "Sector Weekly").random(),
                        "summary" to "New reports have surfaced regarding ${entity.entityValue}. Analysis suggests a significant impact on local frequencies and data stability.",
                        "url" to "https://example.com/news/${entity.entityValue.lowercase().replace(" ", "-")}",
                        "publishedAt" to "Recently"
                    )
                    redisTemplate.opsForValue().set(cacheKey, newsData, Duration.ofMinutes(15)).thenReturn(newsData)
                }
            )
            .flatMap { data ->
                @Suppress("UNCHECKED_CAST")
                val event = ContentInjectionEvent(
                    contentType = "NEWS_ARTICLE",
                    channelId = entity.channelId,
                    data = data as Map<String, String>
                )
                dispatchEvent(event)
            }
    }

    private fun handleSocialSource(entity: EntityMessage): Mono<Void> {
        val cacheKey = "content:social:${entity.entityValue.lowercase().replace(" ", "_")}"
        return redisTemplate.opsForValue().get(cacheKey)
            .switchIfEmpty(
                Mono.defer {
                    val socialData = mapOf(
                        "author" to "@FrequencyObserver",
                        "platform" to "AdaptaSocial",
                        "text" to "Did anyone else notice the resonance patterns with ${entity.entityValue}? Interesting shift in the network today. #adapta #frequency",
                        "url" to "https://example.com/social/post/${(10000..99999).random()}",
                        "likes" to "${(1..9).random()}.${(0..9).random()}k"
                    )
                    redisTemplate.opsForValue().set(cacheKey, socialData, Duration.ofMinutes(15)).thenReturn(socialData)
                }
            )
            .flatMap { data ->
                @Suppress("UNCHECKED_CAST")
                val event = ContentInjectionEvent(
                    contentType = "SOCIAL_POST",
                    channelId = entity.channelId,
                    data = data as Map<String, String>
                )
                dispatchEvent(event)
            }
    }

    private fun handleForumSource(entity: EntityMessage): Mono<Void> {
        val cacheKey = "content:forum:${entity.entityValue.lowercase().replace(" ", "_")}"
        return redisTemplate.opsForValue().get(cacheKey)
            .switchIfEmpty(
                Mono.defer {
                    val forumData = mapOf(
                        "threadTitle" to "Community thoughts on ${entity.entityValue}",
                        "forumName" to "Nexus Hub",
                        "author" to "echo_protocol",
                        "excerpt" to "I've been analyzing the throughput of ${entity.entityValue} and the results are quite unexpected compared to previous benchmarks.",
                        "url" to "https://example.com/forum/thread/${(1000..9999).random()}",
                        "replies" to "${(20..250).random()}"
                    )
                    redisTemplate.opsForValue().set(cacheKey, forumData, Duration.ofMinutes(15)).thenReturn(forumData)
                }
            )
            .flatMap { data ->
                @Suppress("UNCHECKED_CAST")
                val event = ContentInjectionEvent(
                    contentType = "FORUM_POST",
                    channelId = entity.channelId,
                    data = data as Map<String, String>
                )
                dispatchEvent(event)
            }
    }

    private fun dispatchEvent(event: ContentInjectionEvent): Mono<Void> {
        return Mono.fromCallable {
            logger.info("Publishing Content Injection Event: ${event.contentType} for channel ${event.channelId}")
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.CONTENT_INJECTION_EXCHANGE_NAME,
                "",
                event
            )
        }.subscribeOn(Schedulers.boundedElastic()).then()
    }
}
