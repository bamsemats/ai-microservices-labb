package com.example.labb_microservices.gateway

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
import org.springframework.test.web.reactive.server.WebTestClient
import java.util.*

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
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
            // We expect 404 or 500 because the downstream service isn't actually there,
            // but NOT 401 from the filter.
            .expectStatus().is5xxServerError
    }

    @Test
    fun `should reject protected route without token`() {
        webTestClient.get()
            .uri("/some-protected-route")
            .exchange()
            .expectStatus().isUnauthorized
    }

    @Test
    fun `should reject protected route with invalid token`() {
        webTestClient.get()
            .uri("/some-protected-route")
            .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token")
            .exchange()
            .expectStatus().isUnauthorized
    }

    @Test
    fun `should allow protected route with valid token`() {
        val token = Jwts.builder()
            .subject("testuser")
            .expiration(Date(System.currentTimeMillis() + 3600000))
            .signWith(key)
            .compact()

        webTestClient.get()
            .uri("/some-protected-route")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            .exchange()
            // Again, 404/500 is fine as long as it's not 401.
            .expectStatus().is5xxServerError
    }
}
