package com.example.labb_microservices.user_service

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.ApplicationContext
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.testcontainers.containers.MongoDBContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.*

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class UserControllerTests {

    companion object {
        @Container
        @ServiceConnection
        val mongoDBContainer = MongoDBContainer("mongo:7.0")

        private const val SECRET = "a-very-long-and-secure-secret-key-that-is-at-least-256-bits"
        private val KEY = Keys.hmacShaKeyFor(SECRET.toByteArray())
    }

    @Autowired
    private lateinit var context: ApplicationContext

    private lateinit var webTestClient: WebTestClient

    @BeforeEach
    fun setUp() {
        webTestClient = WebTestClient.bindToApplicationContext(context).build()
    }

    @Test
    fun `should register a new user`() {
        val registrationRequest = mapOf(
            "username" to "testuser_" + UUID.randomUUID().toString().take(8),
            "password" to "testpassword"
        )

        webTestClient.post()
            .uri("/register")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(registrationRequest)
            .exchange()
            .expectStatus().isCreated
            .expectBody()
            .jsonPath("$.username").isEqualTo(registrationRequest["username"]!!)
            .jsonPath("$.id").exists()
    }

    @Test
    fun `should reject request to protected endpoint without token`() {
        webTestClient.get()
            .uri("/me")
            .exchange()
            .expectStatus().isUnauthorized
    }

    @Test
    fun `should accept request to protected endpoint with valid token`() {
        val token = Jwts.builder()
            .subject("testuser")
            .claim("tokenType", "access")
            .signWith(KEY)
            .compact()

        webTestClient.get()
            .uri("/me")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            .exchange()
            .expectStatus().isOk
            .expectBody(String::class.java).isEqualTo("authenticated")
    }

    @Test
    fun `should reject request to protected endpoint with invalid token`() {
        val invalidKey = Keys.hmacShaKeyFor("different-secret-key-that-is-at-least-256-bits".toByteArray())
        val token = Jwts.builder()
            .subject("testuser")
            .claim("tokenType", "access")
            .signWith(invalidKey)
            .compact()

        webTestClient.get()
            .uri("/me")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            .exchange()
            .expectStatus().isUnauthorized
    }
}
