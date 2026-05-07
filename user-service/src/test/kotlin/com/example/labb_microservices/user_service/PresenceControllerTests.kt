package com.example.labb_microservices.user_service

import com.example.common.test.BaseIntegrationTest
import com.example.labb_microservices.user_service.model.User
import com.example.labb_microservices.user_service.repository.UserRepository
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import java.util.*

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, properties = [
    "jwt.secret=a-very-long-and-secure-secret-key-that-is-at-least-256-bits",
    "encryption.secret=another-very-long-and-secure-secret-key-32-chars",
    "grpc.server.port=0",
    "grpc.server.security.enabled=false",
    "grpc.server.security.key-store-password=ignored",
    "grpc.server.security.key-password=ignored",
    "grpc.server.security.trust-store-password=ignored"
])
class PresenceControllerTests : BaseIntegrationTest() {

    companion object {
        private const val SECRET = "a-very-long-and-secure-secret-key-that-is-at-least-256-bits"
        private val KEY = Keys.hmacShaKeyFor(SECRET.toByteArray())
    }

    @Autowired
    private lateinit var context: ApplicationContext

    @Autowired
    private lateinit var userRepository: UserRepository

    private lateinit var webTestClient: WebTestClient
    private lateinit var testUser: User

    @BeforeEach
    fun setUp() {
        webTestClient = WebTestClient.bindToApplicationContext(context).build()
        
        // Create a test user in the DB
        val username = "testuser_" + UUID.randomUUID().toString().take(8)
        testUser = userRepository.save(User(username = username, password = "password")).block()!!
    }

    @Test
    fun `should update and get presence status`() {
        val token = Jwts.builder()
            .subject(testUser.username)
            .claim("userId", testUser.id)
            .claim("tokenType", "access")
            .issuedAt(Date())
            .expiration(Date(System.currentTimeMillis() + 3600000))
            .signWith(KEY)
            .compact()

        // Update status to AWAY
        webTestClient.put()
            .uri("/users/status")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("\"AWAY\"")
            .exchange()
            .expectStatus().isOk

        // Get status
        webTestClient.get()
            .uri("/users/${testUser.id}/status")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            .exchange()
            .expectStatus().isOk
            .expectBody(String::class.java)
            .isEqualTo("\"AWAY\"")
    }

    @Test
    fun `should return OFFLINE for unknown user status`() {
        val token = Jwts.builder()
            .subject("someuser")
            .claim("userId", "someid")
            .claim("tokenType", "access")
            .issuedAt(Date())
            .expiration(Date(System.currentTimeMillis() + 3600000))
            .signWith(KEY)
            .compact()

        webTestClient.get()
            .uri("/users/nonexistent/status")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            .exchange()
            .expectStatus().isOk
            .expectBody(String::class.java)
            .isEqualTo("\"OFFLINE\"")
    }
}
