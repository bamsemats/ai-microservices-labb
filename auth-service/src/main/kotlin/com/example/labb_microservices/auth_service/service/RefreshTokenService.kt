package com.example.labb_microservices.auth_service.service

import org.springframework.data.redis.core.ReactiveStringRedisTemplate
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.security.MessageDigest
import java.time.Duration

@Service
class RefreshTokenService(private val redisTemplate: ReactiveStringRedisTemplate) {

    private val refreshTokenPrefix = "refresh_token:"
    private val refreshTokenDuration = Duration.ofDays(7)

    fun saveRefreshToken(userId: String, token: String): Mono<Boolean> {
        val hashedToken = hashToken(token)
        return redisTemplate.opsForValue()
            .set(refreshTokenPrefix + userId, hashedToken, refreshTokenDuration)
    }

    fun getRefreshToken(userId: String): Mono<String> {
        return redisTemplate.opsForValue().get(refreshTokenPrefix + userId)
    }

    fun validateRefreshToken(userId: String, presentedToken: String): Mono<Boolean> {
        return getRefreshToken(userId)
            .map { storedHash ->
                val presentedHash = hashToken(presentedToken)
                MessageDigest.isEqual(storedHash.toByteArray(), presentedHash.toByteArray())
            }
            .defaultIfEmpty(false)
    }

    fun deleteRefreshToken(userId: String): Mono<Boolean> {
        return redisTemplate.opsForValue().delete(refreshTokenPrefix + userId)
    }

    private fun hashToken(token: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(token.toByteArray())
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}
