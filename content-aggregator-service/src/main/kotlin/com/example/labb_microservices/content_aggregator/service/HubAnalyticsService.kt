package com.example.labb_microservices.content_aggregator.service

import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.util.function.Tuple2
import org.springframework.data.domain.Range

data class TrendingChannel(val channelId: String, val score: Double)
data class UserStats(val messagesSent: Long, val aiTokens: Long, val connectionIndex: Int)

@Service
class HubAnalyticsService(private val redisTemplate: ReactiveRedisTemplate<String, Any>) {

    companion object {
        private const val MAX_TRENDING_CHANNELS = 100L
    }

    fun getTrendingChannels(limit: Long = 10): Flux<TrendingChannel> {
        val cappedLimit = limit.coerceIn(1, MAX_TRENDING_CHANNELS)
        val key = "trending:channels"
        
        return redisTemplate.opsForZSet()
            .reverseRangeWithScores(key, Range.closed(0L, cappedLimit - 1))
            .map { tuple ->
                TrendingChannel(
                    channelId = tuple.value as String,
                    score = tuple.score ?: 0.0
                )
            }
    }

    fun getUserStats(userId: String): Mono<UserStats> {
        val messagesKey = "stats:user:$userId:messages"
        val aiTokensKey = "stats:user:$userId:ai_tokens"
        
        fun safeToLong(value: Any?): Long {
            return when (value) {
                is Number -> value.toLong()
                is String -> value.toLongOrNull() ?: 0L
                else -> 0L
            }
        }

        return Mono.zip(
            redisTemplate.opsForValue().get(messagesKey).map { safeToLong(it) }.defaultIfEmpty(0L),
            redisTemplate.opsForValue().get(aiTokensKey).map { safeToLong(it) }.defaultIfEmpty(0L)
        ).map { tuple: Tuple2<Long, Long> ->
            // Calculate a dummy connection index
            val connectionIndex = (tuple.t1 * 0.5 + tuple.t2 * 0.01).coerceAtMost(100.0).toInt()
            UserStats(tuple.t1, tuple.t2, connectionIndex)
        }
    }
}
