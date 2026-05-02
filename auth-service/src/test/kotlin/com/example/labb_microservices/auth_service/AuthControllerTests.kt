package com.example.labb_microservices.auth_service

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
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class AuthControllerTests {

    companion object {
        @Container
        @ServiceConnection
        val redis = GenericContainer("redis:7.0").withExposedPorts(6379)
    }

    @Autowired
    private lateinit var context: ApplicationContext

    private lateinit var webTestClient: WebTestClient

    @MockitoBean
    private lateinit var userGrpcClient: UserGrpcClient

    @BeforeEach
    fun setUp() {
        webTestClient = WebTestClient.bindToApplicationContext(context).build()
    }

    @Test
    fun `should login and return jwt`() {
        val loginRequest = LoginRequest("testuser", "testpassword")
        val grpcResponse = CredentialsResponse.newBuilder()
            .setValid(true)
            .setUserId("123")
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
            .jsonPath("$.userId").isEqualTo("123")
            .jsonPath("$.username").isEqualTo("testuser")
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
