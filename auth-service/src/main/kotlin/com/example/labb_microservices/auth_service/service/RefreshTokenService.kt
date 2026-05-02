package com.example.labb_microservices.auth_service.service

import org.springframework.data.redis.core.ReactiveStringRedisTemplate
import org.springframework.data.redis.core.script.DefaultRedisScript
import org.springframework.data.redis.core.script.RedisScript
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.security.MessageDigest
import java.time.Duration

@Service
class RefreshTokenService(private val redisTemplate: ReactiveStringRedisTemplate) {

    private val refreshTokenPrefix = "refresh_token:"
    private val refreshTokenDuration = Duration.ofDays(7)

    private val rotateScript: RedisScript<Boolean> = DefaultRedisScript(
        """
        if redis.call('get', KEYS[1]) == ARGV[1] then
            redis.call('set', KEYS[1], ARGV[2], 'EX', ARGV[3])
            return true
        else
            return false
        end
        """.trimIndent(),
        Boolean::class.java
    )

    fun saveRefreshToken(userId: String, token: String): Mono<Boolean> {
        val hashedToken = hashToken(token)
        return redisTemplate.opsForValue()
            .set(refreshTokenPrefix + userId, hashedToken, refreshTokenDuration)
    }

    fun rotateRefreshToken(userId: String, oldToken: String, newToken: String): Mono<Boolean> {
        val oldHash = hashToken(oldToken)
        val newHash = hashToken(newToken)
        return redisTemplate.execute(
            rotateScript,
            listOf(refreshTokenPrefix + userId),
            listOf(oldHash, newHash, refreshTokenDuration.seconds.toString())
        ).next()
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
