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
    private val logger = org.slf4j.LoggerFactory.getLogger(JwtTokenValidator::class.java)
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
            logger.debug("Token validation failed: {}", e.message)
            false
        }
    }

    fun getAuthentication(token: String): String? {
        return getClaims(token)?.subject
    }

    fun getUserIdFromToken(token: String): String? {
        return getClaims(token)?.get("userId", String::class.java)
    }

    fun getRolesFromToken(token: String): List<String> {
        val claims = getClaims(token) ?: return emptyList()
        @Suppress("UNCHECKED_CAST")
        return claims.get("roles", List::class.java) as? List<String> ?: emptyList()
    }

    private fun getClaims(token: String): io.jsonwebtoken.Claims? {
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

}
