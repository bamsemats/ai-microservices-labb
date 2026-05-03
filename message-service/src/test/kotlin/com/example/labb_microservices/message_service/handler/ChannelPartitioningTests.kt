package com.example.labb_microservices.message_service.handler

import com.example.labb_microservices.common.security.JwtTokenValidator
import com.example.labb_microservices.message_service.client.UserGrpcClient
import com.example.labb_microservices.message_service.controller.BroadcastRequest
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient
import org.testcontainers.containers.MongoDBContainer
import org.testcontainers.containers.RabbitMQContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import reactor.core.publisher.Mono
import java.net.URI
import java.util.concurrent.CopyOnWriteArrayList

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = [
    "jwt.secret=a-very-long-and-secure-secret-key-that-is-at-least-256-bits",
    "encryption.secret=another-very-long-and-secure-secret-key-32-chars",
    "grpc.server.port=0"
])
@Testcontainers
class ChannelPartitioningTests {

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

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private lateinit var jwtTokenValidator: JwtTokenValidator

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private lateinit var userGrpcClient: UserGrpcClient

    @Test
    fun `messages sent to Channel-A should not leak to Channel-B`() {
        val userA = "user-a"
        val tokenA = "token-a"
        val userB = "user-b"
        val tokenB = "token-b"

        setupMock(userA, tokenA)
        setupMock(userB, tokenB)

        val receivedA = CopyOnWriteArrayList<String>()
        val receivedB = CopyOnWriteArrayList<String>()

        val client = ReactorNettyWebSocketClient()
        
        // Connect User A to Channel-A
        val sessionAMono = client.execute(URI("ws://localhost:$port/ws/messages?token=$tokenA&channel=Channel-A")) { session ->
            session.receive().map { it.payloadAsText }.doOnNext { receivedA.add(it) }.then()
        }

        // Connect User B to Channel-B
        val sessionBMono = client.execute(URI("ws://localhost:$port/ws/messages?token=$tokenB&channel=Channel-B")) { session ->
            session.receive().map { it.payloadAsText }.doOnNext { receivedB.add(it) }.then()
        }

        val adminToken = "admin-token"
        setupMock("admin", adminToken, true)

        // Run sessions in background
        val disposableA = sessionAMono.subscribe()
        val disposableB = sessionBMono.subscribe()

        try {
            // Wait for connections to stabilize
            Thread.sleep(3000)

            // Send broadcast to Channel-A
            webTestClient.post()
                .uri("/messages/broadcast")
                .header("Authorization", "Bearer $adminToken")
                .bodyValue(BroadcastRequest(content = "Hello Channel A", channelId = "Channel-A"))
                .exchange()
                .expectStatus().isOk

            // Wait for message delivery
            Thread.sleep(3000)

            // Verify
            val hasA = receivedA.any { it.contains("Hello Channel A") }
            val hasB = receivedB.any { it.contains("Hello Channel A") }
            
            println("Received A: $receivedA")
            println("Received B: $receivedB")

            assert(hasA) { "User A should have received the message" }
            assert(!hasB) { "User B should NOT have received the message" }

        } finally {
            disposableA.dispose()
            disposableB.dispose()
        }
    }

    private fun setupMock(userId: String, token: String, isAdmin: Boolean = false) {
        `when`(jwtTokenValidator.validateToken(token)).thenReturn(true)
        `when`(jwtTokenValidator.getUserIdFromToken(token)).thenReturn(userId)
        `when`(jwtTokenValidator.getAuthentication(token)).thenReturn(userId)
        
        val roles = if (isAdmin) listOf("ROLE_ADMIN", "ROLE_USER") else listOf("ROLE_USER")
        `when`(jwtTokenValidator.getRolesFromToken(token)).thenReturn(roles)

        `when`(userGrpcClient.getUser(userId)).thenReturn(Mono.just(
            com.example.labb_microservices.proto.UserResponse.newBuilder()
                .setUserId(userId)
                .setEnabled(true)
                .build()
        ))
    }
}
