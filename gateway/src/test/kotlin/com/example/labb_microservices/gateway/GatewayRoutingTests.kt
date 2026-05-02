package com.example.labb_microservices.gateway

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
import org.springframework.test.web.reactive.server.WebTestClient
import java.util.*

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = [
    "services.auth=http://localhost:12345",
    "services.user=http://localhost:12346",
    "services.message=http://localhost:12347"
])
class GatewayRoutingTests {

    @Autowired
    private lateinit var webTestClient: WebTestClient

    private val secret = "a-very-long-and-secure-secret-key-that-is-at-least-256-bits"
    private val key = Keys.hmacShaKeyFor(secret.toByteArray())

    @Test
    fun `should allow login without token`() {
        webTestClient.post()
            .uri("/login")
            .exchange()
            // We expect 503 or 504 because the downstream service isn't there
            .expectStatus().is5xxServerError
    }

    @Test
    fun `should reject protected route without token`() {
        webTestClient.get()
            .uri("/users/me")
            .exchange()
            .expectStatus().isUnauthorized
    }

    @Test
    fun `should reject protected route with invalid token`() {
        webTestClient.get()
            .uri("/users/me")
            .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token")
            .exchange()
            .expectStatus().isUnauthorized
    }

    @Test
    fun `should allow protected route with valid token`() {
        val token = Jwts.builder()
            .subject("testuser")
            .claim("tokenType", "access")
            .expiration(Date(System.currentTimeMillis() + 3600000))
            .signWith(key)
            .compact()

        webTestClient.get()
            .uri("/users/me")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            .exchange()
            // We expect 503 or 504 because the downstream service isn't there
            .expectStatus().is5xxServerError
    }

    @Test
    fun `should allow protected route with valid token in query param`() {
        val token = Jwts.builder()
            .subject("testuser")
            .claim("tokenType", "access")
            .expiration(Date(System.currentTimeMillis() + 3600000))
            .signWith(key)
            .compact()

        webTestClient.get()
            .uri("/ws/messages?token=$token")
            .exchange()
            // We expect 503 or 504 because the downstream service isn't there
            .expectStatus().is5xxServerError
    }

    @Test
    fun `should reject protected route with invalid token in query param`() {
        webTestClient.get()
            .uri("/ws/messages?token=invalid-token")
            .exchange()
            .expectStatus().isUnauthorized
    }

    @Test
    fun `should reject query param token for non-ws routes`() {
        val token = Jwts.builder()
            .subject("testuser")
            .claim("tokenType", "access")
            .expiration(Date(System.currentTimeMillis() + 3600000))
            .signWith(key)
            .compact()

        webTestClient.get()
            .uri("/users/me?token=$token")
            .exchange()
            .expectStatus().isUnauthorized
    }
}
