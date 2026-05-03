package com.example.labb_microservices.common.security

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.*
import javax.crypto.SecretKey

class JwtInteroperabilityTest {

    private val secret = "a-very-long-and-secure-secret-key-that-is-at-least-256-bits"
    private val key: SecretKey = Keys.hmacShaKeyFor(secret.toByteArray())

    @Test
    fun `should validate token generated with JJWT 0-12-x patterns`() {
        val now = Date()
        val expiryDate = Date(now.time + 3600000)

        val token = Jwts.builder()
            .subject("testuser")
            .claim("userId", "12345")
            .claim("tokenType", "access")
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(key)
            .compact()

        val claims = Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .payload

        assertNotNull(claims)
        assertTrue(claims.subject == "testuser")
        assertTrue(claims["tokenType"] == "access")
    }
}
