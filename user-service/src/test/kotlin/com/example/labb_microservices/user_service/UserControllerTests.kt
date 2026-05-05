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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = [
    "jwt.secret=a-very-long-and-secure-secret-key-that-is-at-least-256-bits",
    "encryption.secret=another-very-long-and-secure-secret-key-32-chars",
    "grpc.server.port=0",
    "grpc.server.security.enabled=false",
    "grpc.server.security.key-store-password=ignored",
    "grpc.server.security.key-password=ignored",
    "grpc.server.security.trust-store-password=ignored"
])
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
            "password" to "testpassword",
            "email" to "test@example.com"
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
            .uri("/users/me")
            .exchange()
            .expectStatus().isUnauthorized
    }

    @Test
    fun `should accept request to protected endpoint with valid token`() {
        val username = "testuser"
        val userId = "test-user-id"
        
        // Register user first so they exist in DB
        val userRepository = context.getBean(com.example.labb_microservices.user_service.repository.UserRepository::class.java)
        userRepository.save(com.example.labb_microservices.user_service.model.User(
            id = userId,
            username = username,
            password = "pw"
        )).block()

        val token = Jwts.builder()
            .subject(userId)
            .claim("userId", userId)
            .claim("tokenType", "access")
            .issuedAt(Date())
            .expiration(Date(System.currentTimeMillis() + 3600000))
            .signWith(KEY)
            .compact()

        webTestClient.get()
            .uri("/users/me")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.id").isEqualTo(userId)
            .jsonPath("$.username").isEqualTo(username)
    }

    @Test
    fun `should update user profile`() {
        val userId = "test-profile-user"
        val username = "profileuser"
        
        // Register user first
        val userRepository = context.getBean(com.example.labb_microservices.user_service.repository.UserRepository::class.java)
        userRepository.save(com.example.labb_microservices.user_service.model.User(
            id = userId,
            username = username,
            password = "pw"
        )).block()

        val token = Jwts.builder()
            .subject(userId)
            .claim("userId", userId)
            .claim("tokenType", "access")
            .issuedAt(Date())
            .expiration(Date(System.currentTimeMillis() + 3600000))
            .signWith(KEY)
            .compact()

        val profileRequest = mapOf(
            "displayName" to "My Display Name",
            "bio" to "My Bio"
        )

        webTestClient.put()
            .uri("/users/profile")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(profileRequest)
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.displayName").isEqualTo("My Display Name")
            .jsonPath("$.bio").isEqualTo("My Bio")
    }

    @Test
    fun `should reject request to protected endpoint with invalid token`() {
        val invalidKey = Keys.hmacShaKeyFor("different-secret-key-that-is-at-least-256-bits".toByteArray())
        val token = Jwts.builder()
            .subject("testuser")
            .claim("tokenType", "access")
            .issuedAt(Date())
            .expiration(Date(System.currentTimeMillis() + 3600000))
            .signWith(invalidKey)
            .compact()

        webTestClient.get()
            .uri("/users/me")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            .exchange()
            .expectStatus().isUnauthorized
    }

    @Test
    fun `should register user with encrypted email and return decrypted email`() {
        val username = "testuser_" + UUID.randomUUID().toString().take(8)
        val email = "test@example.com"
        val registrationRequest = mapOf(
            "username" to username,
            "password" to "testpassword",
            "email" to email
        )

        val response = webTestClient.post()
            .uri("/register")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(registrationRequest)
            .exchange()
            .expectStatus().isCreated
            .expectBody(Map::class.java)
            .returnResult()
            .responseBody!!

        // Verify API response has plain text email
        org.junit.jupiter.api.Assertions.assertEquals(email, response["email"])
        
        // Verify database has encrypted email (we can check via repository directly)
        val userRepository = context.getBean(com.example.labb_microservices.user_service.repository.UserRepository::class.java)
        val userId = response["id"] as String
        
        // We use stepVerifier because it's reactive
        reactor.test.StepVerifier.create(userRepository.findById(userId))
            .assertNext { user ->
                org.junit.jupiter.api.Assertions.assertNotEquals(email, user.email)
                org.junit.jupiter.api.Assertions.assertTrue(user.email!!.length > 20) // Base64 ciphertext
                org.junit.jupiter.api.Assertions.assertNotNull(user.emailHash)
            }
            .verifyComplete()
    }
}
