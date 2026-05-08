package com.example.labb_microservices.auth_service

import com.example.common.test.BaseIntegrationTest
import com.example.labb_microservices.auth_service.client.UserGrpcClient
import com.example.labb_microservices.auth_service.controller.LoginRequest
import com.example.labb_microservices.proto.CredentialsResponse
import reactor.core.publisher.Mono
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.context.ApplicationContext
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient

import com.example.labb_microservices.auth_service.controller.LoginResponse
import com.example.labb_microservices.auth_service.service.RefreshTokenService
import java.util.UUID

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = [
    "jwt.secret=a-very-long-and-secure-secret-key-that-is-at-least-256-bits",
    "encryption.secret=another-very-long-and-secure-secret-key-32-chars",
    "grpc.server.port=0"
])
class AuthControllerTests : BaseIntegrationTest() {

    @Autowired
    private lateinit var context: ApplicationContext

    @Autowired
    private lateinit var refreshTokenService: RefreshTokenService

    private lateinit var webTestClient: WebTestClient

    @MockitoBean
    private lateinit var userGrpcClient: UserGrpcClient

    @BeforeEach
    fun setUp() {
        webTestClient = WebTestClient.bindToApplicationContext(context).build()
        // Clear Redis state for isolation
        val redisTemplate = context.getBean("reactiveStringRedisTemplate") as org.springframework.data.redis.core.ReactiveStringRedisTemplate
        redisTemplate.execute { it.serverCommands().flushAll() }.blockFirst()
    }

    @Test
    fun `should login and return jwt`() {
        val userId = UUID.randomUUID().toString()
        val loginRequest = LoginRequest("testuser", "testpassword")
        val grpcResponse = CredentialsResponse.newBuilder()
            .setValid(true)
            .setUserId(userId)
            .setUsername("testuser")
            .build()

        `when`(userGrpcClient.validateCredentials("testuser", "testpassword"))
            .thenReturn(Mono.just(grpcResponse))

        webTestClient.post()
            .uri("/login")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(loginRequest)
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.accessToken").exists()
            .jsonPath("$.refreshToken").exists()
            .jsonPath("$.userId").isEqualTo(userId)
            .jsonPath("$.username").isEqualTo("testuser")
    }

    @Test
    fun `should refresh token`() {
        val userId = UUID.randomUUID().toString()
        val loginRequest = LoginRequest("testuser", "testpassword")
        val grpcResponse = CredentialsResponse.newBuilder()
            .setValid(true)
            .setUserId(userId)
            .setUsername("testuser")
            .build()

        `when`(userGrpcClient.validateCredentials("testuser", "testpassword"))
            .thenReturn(Mono.just(grpcResponse))

        val loginResponse = webTestClient.post()
            .uri("/login")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(loginRequest)
            .exchange()
            .expectStatus().isOk
            .expectBody(LoginResponse::class.java)
            .returnResult()
            .responseBody!!

        val refreshToken = loginResponse.refreshToken

        val refreshRequest = mapOf("userId" to userId, "refreshToken" to refreshToken)

        webTestClient.post()
            .uri("/refresh")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(refreshRequest)
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.accessToken").exists()
            .jsonPath("$.refreshToken").exists()
    }

    @Test
    fun `should logout and invalidate refresh token`() {
        val userId = UUID.randomUUID().toString()
        val loginRequest = LoginRequest("testuser", "testpassword")
        val grpcResponse = CredentialsResponse.newBuilder()
            .setValid(true)
            .setUserId(userId)
            .setUsername("testuser")
            .build()

        `when`(userGrpcClient.validateCredentials("testuser", "testpassword"))
            .thenReturn(Mono.just(grpcResponse))

        val loginResponse = webTestClient.post()
            .uri("/login")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(loginRequest)
            .exchange()
            .expectStatus().isOk
            .expectBody(LoginResponse::class.java)
            .returnResult()
            .responseBody!!

        val refreshToken = loginResponse.refreshToken

        // Logout
        val logoutRequest = mapOf("userId" to userId, "refreshToken" to refreshToken)
        webTestClient.post()
            .uri("/logout")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(logoutRequest)
            .exchange()
            .expectStatus().isNoContent

        // Try to refresh - should fail
        val refreshRequest = mapOf("userId" to userId, "refreshToken" to refreshToken)
        webTestClient.post()
            .uri("/refresh")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(refreshRequest)
            .exchange()
            .expectStatus().isUnauthorized
    }

    @Test
    fun `should return unauthorized for invalid credentials`() {
        val loginRequest = LoginRequest("testuser", "wrongpassword")
        val grpcResponse = CredentialsResponse.newBuilder()
            .setValid(false)
            .build()

        `when`(userGrpcClient.validateCredentials("testuser", "wrongpassword"))
            .thenReturn(Mono.just(grpcResponse))

        webTestClient.post()
            .uri("/login")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(loginRequest)
            .exchange()
            .expectStatus().isUnauthorized
    }
}
