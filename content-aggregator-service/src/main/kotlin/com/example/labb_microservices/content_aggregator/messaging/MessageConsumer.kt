package com.example.labb_microservices.content_aggregator.messaging

import com.example.labb_microservices.content_aggregator.model.Message
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class MessageConsumer(
    private val redisTemplate: ReactiveRedisTemplate<String, Any>
) {

    private val logger = LoggerFactory.getLogger(MessageConsumer::class.java)

    @RabbitListener(queues = ["#{deliveryQueue.name}"])
    fun processDeliveredMessage(message: Message): Mono<Void> {
        val messageId = message.id
        val channelId = message.channelId
        val senderId = message.senderId

        if (messageId.isNullOrBlank()) {
            return Mono.empty()
        }

        val processedKey = "processed:message:$messageId"
        val trendingKey = "trending:channels"
        val userStatsKey = "stats:user:$senderId:messages"

        return redisTemplate.opsForValue()
            .setIfAbsent(processedKey, "true", java.time.Duration.ofHours(24))
            .flatMap { isNew ->
                if (isNew == true) {
                    val trendingUpdate = if (channelId.isNotBlank() && channelId != "null") {
                        redisTemplate.opsForZSet().incrementScore(trendingKey, channelId, 1.0)
                    } else {
                        Mono.just(0.0)
                    }

                    val userUpdate = if (senderId.isNotBlank()) {
                        redisTemplate.opsForValue().increment(userStatsKey)
                    } else {
                        Mono.just(0L)
                    }

                    Mono.zip(trendingUpdate, userUpdate)
                        .doOnNext { logger.info("Updated stats for user $senderId and channel $channelId (Message ID: $messageId)") }
                        .then()
                } else {
                    logger.debug("Skipping duplicate message: $messageId")
                    Mono.empty()
                }
            }
    }

    @RabbitListener(queues = ["#{aiResponseQueue.name}"])
    fun processAiResponse(message: Message): Mono<Void> {
        val receiverId = message.receiverId
        if (receiverId.isBlank() || receiverId == "all") {
            return Mono.empty()
        }

        // Simulate token generation based on content length
        val tokens = (message.content.length * 0.75).toLong()
        val aiStatsKey = "stats:user:$receiverId:ai_tokens"

        logger.info("Updating AI tokens for user $receiverId: +$tokens")
        return redisTemplate.opsForValue().increment(aiStatsKey, tokens)
            .then()
    }
}
