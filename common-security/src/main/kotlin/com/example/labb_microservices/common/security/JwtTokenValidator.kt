package com.example.labb_microservices.common.security

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import javax.crypto.SecretKey

@Component
class JwtTokenValidator(
    @Value("\${jwt.secret}")
    private val secret: String
) {
    private lateinit var key: SecretKey

    @PostConstruct
    fun init() {
        if (secret.isBlank()) {
            throw IllegalStateException("JWT secret cannot be blank")
        }
        key = Keys.hmacShaKeyFor(secret.toByteArray())
    }

    fun validateToken(token: String): Boolean {
        return try {
            val claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .payload
            claims["tokenType"] == "access"
        } catch (e: Exception) {
            false
        }
    }

    fun getAuthentication(token: String): String? {
        return try {
            val claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .payload

            val tokenType = claims["tokenType"]
            if (tokenType == "access") {
                claims.subject
            } else {
                org.slf4j.LoggerFactory.getLogger(JwtTokenValidator::class.java)
                    .warn("Invalid token type: {}", tokenType)
                null
            }
        } catch (e: Exception) {
            org.slf4j.LoggerFactory.getLogger(JwtTokenValidator::class.java)
                .error("Token validation failed: {}", e.message)
            null
        }
    }

}
