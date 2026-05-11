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
    private val userPresencePrefix = "presence:active:"
    private val botPresencePrefix = "presence:static:"

    fun setUserOnline(userId: String): Mono<Boolean> {
        return redisTemplate.opsForValue()
            .set("${userPresencePrefix}$userId", "ONLINE", Duration.ofMinutes(5))
    }

    fun setBotOnline(userId: String): Mono<Boolean> {
        return redisTemplate.opsForValue()
            .set("${botPresencePrefix}$userId", "ONLINE")
    }

    fun setUserOffline(userId: String): Mono<Long> {
        return redisTemplate.delete("${userPresencePrefix}$userId")
    }

    fun isUserOnline(userId: String): Mono<Boolean> {
        return redisTemplate.hasKey("${userPresencePrefix}$userId")
            .flatMap { isOnline ->
                if (isOnline) Mono.just(true)
                else redisTemplate.hasKey("${botPresencePrefix}$userId")
            }
    }

    fun getAllOnlineUsers(): Flux<String> {
        val userOptions = ScanOptions.scanOptions().match("${userPresencePrefix}*").count(1000).build()
        val botOptions = ScanOptions.scanOptions().match("${botPresencePrefix}*").count(1000).build()
        
        return redisTemplate.scan(userOptions)
            .map { it.removePrefix(userPresencePrefix) }
            .mergeWith(
                redisTemplate.scan(botOptions)
                    .map { it.removePrefix(botPresencePrefix) }
            )
            .distinct()
    }
}
