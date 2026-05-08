package com.example.labb_microservices.auth_service.service

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.*
import javax.crypto.SecretKey

@Service
class JwtService(
    @Value("\${jwt.secret}")
    private val secret: String
) {
    private lateinit var key: SecretKey
    private val accessTokenExpirationTimeInMs = 900000 // 15 minutes
    private val refreshTokenExpirationTimeInMs = 604800000 // 7 days

    @PostConstruct
    fun init() {
        if (secret.isBlank()) {
            throw IllegalStateException("JWT secret cannot be blank")
        }
        key = Keys.hmacShaKeyFor(secret.toByteArray())
    }

    fun generateAccessToken(username: String, userId: String): String {
        return generateToken(username, userId, accessTokenExpirationTimeInMs, "access")
    }

    fun generateRefreshToken(username: String, userId: String): String {
        return generateToken(username, userId, refreshTokenExpirationTimeInMs, "refresh")
    }

    private fun generateToken(username: String, userId: String, expirationMs: Int, tokenType: String): String {
        val now = Date()
        val expiryDate = Date(now.time + expirationMs)

        return Jwts.builder()
            .subject(username)
            .claim("userId", userId)
            .claim("tokenType", tokenType)
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

    fun validateToken(token: String, expectedType: String = "access"): Boolean {
        val claims = getClaims(token) ?: return false
        return claims["tokenType"] == expectedType
    }
}
