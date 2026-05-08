package com.example.labb_microservices.user_service.repository

import com.example.labb_microservices.user_service.model.PresenceStatus
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Duration

@Component
class PresenceTracker(private val redisTemplate: ReactiveRedisTemplate<String, String>) {

    private val presenceKeyPrefix = "presence:"

    fun setStatus(userId: String, status: PresenceStatus): Mono<Boolean> {
        return redisTemplate.opsForValue()
            .set(presenceKey(userId), status.name, Duration.ofDays(7))
    }

    fun getStatus(userId: String): Mono<PresenceStatus> {
        return redisTemplate.opsForValue()
            .get(presenceKey(userId))
            .map { PresenceStatus.valueOf(it) }
            .onErrorResume(IllegalArgumentException::class.java) { Mono.just(PresenceStatus.OFFLINE) }
            .defaultIfEmpty(PresenceStatus.OFFLINE)
    }

    fun getAllPresences(): Flux<Pair<String, PresenceStatus>> {
        return redisTemplate.keys("$presenceKeyPrefix*")
            .flatMap { key ->
                val userId = key.removePrefix(presenceKeyPrefix)
                getStatus(userId).map { userId to it }
            }
    }

    private fun presenceKey(userId: String) = "$presenceKeyPrefix$userId"
}
