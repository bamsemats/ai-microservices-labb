package com.example.labb_microservices.auth_service.service

import org.springframework.data.redis.core.ReactiveStringRedisTemplate
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.time.Duration

@Service
class RefreshTokenService(private val redisTemplate: ReactiveStringRedisTemplate) {

    private val refreshTokenPrefix = "refresh_token:"
    private val refreshTokenDuration = Duration.ofDays(7)

    fun saveRefreshToken(userId: String, token: String): Mono<Boolean> {
        return redisTemplate.opsForValue()
            .set(refreshTokenPrefix + userId, token, refreshTokenDuration)
    }

    fun getRefreshToken(userId: String): Mono<String> {
        return redisTemplate.opsForValue().get(refreshTokenPrefix + userId)
    }

    fun deleteRefreshToken(userId: String): Mono<Boolean> {
        return redisTemplate.opsForValue().delete(refreshTokenPrefix + userId)
    }
}
