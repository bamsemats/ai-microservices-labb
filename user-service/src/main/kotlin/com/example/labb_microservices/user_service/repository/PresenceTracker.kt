package com.example.labb_microservices.user_service.repository

import com.example.labb_microservices.user_service.model.PresenceStatus
import org.springframework.data.redis.core.ReactiveStringRedisTemplate
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Duration

@Component
class PresenceTracker(private val redisTemplate: ReactiveStringRedisTemplate) {

    private val userPresencePrefix = "presence:active:"
    private val botPresencePrefix = "presence:static:"

    fun setStatus(userId: String, status: PresenceStatus, isBot: Boolean = false): Mono<Boolean> {
        val prefix = if (isBot) botPresencePrefix else userPresencePrefix
        val oppositePrefix = if (isBot) userPresencePrefix else botPresencePrefix
        val ttl = if (isBot) Duration.ofDays(30) else Duration.ofDays(7)
        return redisTemplate.opsForValue()
            .set(presenceKey(userId, prefix), status.name, ttl)
            .flatMap { result ->
                redisTemplate.delete(presenceKey(userId, oppositePrefix))
                    .thenReturn(result)
            }
    }

    fun getStatus(userId: String): Mono<PresenceStatus> {
        return redisTemplate.opsForValue()
            .get(presenceKey(userId, userPresencePrefix))
            .switchIfEmpty(redisTemplate.opsForValue().get(presenceKey(userId, botPresencePrefix)))
            .map { PresenceStatus.valueOf(it) }
            .onErrorResume(IllegalArgumentException::class.java) { Mono.just(PresenceStatus.OFFLINE) }
            .defaultIfEmpty(PresenceStatus.OFFLINE)
    }

    fun getAllPresences(): Flux<Pair<String, PresenceStatus>> {
        val userOptions = org.springframework.data.redis.core.ScanOptions.scanOptions().match("${userPresencePrefix}*").count(1000).build()
        val botOptions = org.springframework.data.redis.core.ScanOptions.scanOptions().match("${botPresencePrefix}*").count(1000).build()
        
        return redisTemplate.scan(userOptions)
            .mergeWith(redisTemplate.scan(botOptions))
            .flatMap { key ->
                val isBot = key.startsWith(botPresencePrefix)
                val prefix = if (isBot) botPresencePrefix else userPresencePrefix
                val userId = key.removePrefix(prefix)
                getStatus(userId).map { userId to it }
            }
    }

    private fun presenceKey(userId: String, prefix: String) = "$prefix$userId"
}
