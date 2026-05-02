package com.example.labb_microservices.auth_service.service

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.*
import javax.crypto.SecretKey

@Service
class JwtService(
    @Value("\${jwt.secret:a-very-long-and-secure-secret-key-that-is-at-least-256-bits}")
    private val secret: String
) {
    private val key: SecretKey = Keys.hmacShaKeyFor(secret.toByteArray())
    private val accessTokenExpirationTimeInMs = 900000 // 15 minutes
    private val refreshTokenExpirationTimeInMs = 604800000 // 7 days

    fun generateAccessToken(username: String, userId: String): String {
        return generateToken(username, userId, accessTokenExpirationTimeInMs)
    }

    fun generateRefreshToken(username: String, userId: String): String {
        return generateToken(username, userId, refreshTokenExpirationTimeInMs)
    }

    private fun generateToken(username: String, userId: String, expirationMs: Int): String {
        val now = Date()
        val expiryDate = Date(now.time + expirationMs)

        return Jwts.builder()
            .subject(username)
            .claim("userId", userId)
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(key)
            .compact()
    }

    fun getClaims(token: String): io.jsonwebtoken.Claims? {
        return try {
            Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .payload
        } catch (e: Exception) {
            null
        }
    }

    fun validateToken(token: String): Boolean {
        return getClaims(token) != null
    }
}
