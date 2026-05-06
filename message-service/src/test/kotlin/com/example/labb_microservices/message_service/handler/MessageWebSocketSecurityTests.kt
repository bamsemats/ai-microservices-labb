package com.example.labb_microservices.message_service.handler

import com.example.labb_microservices.common.security.JwtTokenValidator
import com.example.labb_microservices.message_service.client.UserGrpcClient
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.http.HttpHeaders
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient
import org.testcontainers.containers.MongoDBContainer
import org.testcontainers.containers.RabbitMQContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.net.URI
import java.time.Duration

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = [
    "jwt.secret=a-very-long-and-secure-secret-key-that-is-at-least-256-bits",
    "encryption.secret=another-very-long-and-secure-secret-key-32-chars",
    "grpc.server.port=0",
    "auth.cache.ttl=0"
])
@Testcontainers
class MessageWebSocketSecurityTests {

    companion object {
        @Container
        @ServiceConnection
        val mongodb = MongoDBContainer("mongo:6.0.4")

        @Container
        @ServiceConnection
        val rabbitmq = RabbitMQContainer("rabbitmq:3.11-management")
    }

    @LocalServerPort
    private var port: Int = 0

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private lateinit var jwtTokenValidator: JwtTokenValidator

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private lateinit var userGrpcClient: UserGrpcClient

    @Test
    fun `should close websocket session when user becomes disabled`() {
        val userId = "test-user"
        val token = "valid-token"

        val claims = org.mockito.Mockito.mock(io.jsonwebtoken.Claims::class.java)
        org.mockito.Mockito.`when`(claims.get("userId", String::class.java)).thenReturn(userId)
        org.mockito.Mockito.`when`(jwtTokenValidator.getValidatedClaims(token)).thenReturn(claims)

        `when`(jwtTokenValidator.validateToken(token)).thenReturn(true)
        `when`(jwtTokenValidator.getAuthentication(token)).thenReturn(userId)
        `when`(jwtTokenValidator.getUserIdFromToken(token)).thenReturn(userId)
        
        // Return enabled then disabled on subsequent calls
        `when`(userGrpcClient.getUser(userId))
            .thenReturn(Mono.just(com.example.labb_microservices.proto.UserResponse.newBuilder()
                .setUserId(userId)
                .setEnabled(true)
                .build()))
            .thenReturn(Mono.just(com.example.labb_microservices.proto.UserResponse.newBuilder()
                .setUserId(userId)
                .setEnabled(false)
                .build()))

        val client = ReactorNettyWebSocketClient()
        val uri = URI("ws://localhost:$port/ws/messages")
        val headers = HttpHeaders()
        headers.add("Authorization", "Bearer $token")

        val sessionMono = client.execute(uri, headers) { session ->
            session.receive()
                .doOnNext { println("Received: ${it.payloadAsText}") }
                .then()
        }

        StepVerifier.create(sessionMono)
            .expectComplete()
            .verify(Duration.ofSeconds(25)) // Wait for interval check
    }

    @Test
    fun `should close websocket session when token becomes invalid`() {
        val userId = "test-user"
        val token = "valid-token"

        val claims = org.mockito.Mockito.mock(io.jsonwebtoken.Claims::class.java)
        org.mockito.Mockito.`when`(claims.get("userId", String::class.java)).thenReturn(userId)
        org.mockito.Mockito.`when`(jwtTokenValidator.getValidatedClaims(token)).thenReturn(claims).thenReturn(null)

        `when`(jwtTokenValidator.validateToken(token)).thenReturn(true).thenReturn(false)
        `when`(jwtTokenValidator.getAuthentication(token)).thenReturn(userId)
        `when`(jwtTokenValidator.getUserIdFromToken(token)).thenReturn(userId)
        `when`(userGrpcClient.getUser(userId)).thenReturn(Mono.just(com.example.labb_microservices.proto.UserResponse.newBuilder()
            .setUserId(userId)
            .setEnabled(true)
            .build()))

        val client = ReactorNettyWebSocketClient()
        val uri = URI("ws://localhost:$port/ws/messages")
        val headers = HttpHeaders()
        headers.add("Authorization", "Bearer $token")

        val sessionMono = client.execute(uri, headers) { session ->
            session.receive()
                .doOnNext { println("Received: ${it.payloadAsText}") }
                .then()
        }

        StepVerifier.create(sessionMono)
            .expectComplete()
            .verify(Duration.ofSeconds(25)) // Wait for periodic check
    }
}
