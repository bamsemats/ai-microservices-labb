package com.example.labb_microservices.message_service.service

import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.core.ScanOptions
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Duration

@Service
class PresenceService(
    private val redisTemplate: ReactiveRedisTemplate<String, String>
) {
    private val presenceKeyPrefix = "presence:"

    fun setUserOnline(userId: String): Mono<Boolean> {
        return redisTemplate.opsForValue()
            .set("${presenceKeyPrefix}$userId", "ONLINE", Duration.ofDays(7))
    }

    fun setUserOffline(userId: String): Mono<Long> {
        return redisTemplate.delete("${presenceKeyPrefix}$userId")
    }

    fun isUserOnline(userId: String): Mono<Boolean> {
        return redisTemplate.hasKey("${presenceKeyPrefix}$userId")
    }

    fun getAllOnlineUsers(): Flux<String> {
        val options = ScanOptions.scanOptions().match("${presenceKeyPrefix}*").count(1000).build()
        return redisTemplate.scan(options)
            .map { it.removePrefix(presenceKeyPrefix) }
    }
}
