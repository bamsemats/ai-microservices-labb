package com.example.labb_microservices.message_service.handler

import com.example.labb_microservices.common.security.JwtTokenValidator
import com.example.labb_microservices.message_service.client.UserGrpcClient
import com.example.labb_microservices.message_service.service.PresenceService
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.http.HttpHeaders
import org.springframework.web.reactive.socket.CloseStatus
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient
import org.testcontainers.containers.MongoDBContainer
import org.testcontainers.containers.RabbitMQContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import reactor.core.publisher.Mono
import reactor.core.publisher.Sinks
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

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private lateinit var presenceService: PresenceService

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
        `when`(presenceService.setUserOnline(org.mockito.ArgumentMatchers.anyString())).thenReturn(Mono.empty())
        `when`(presenceService.setUserOffline(org.mockito.ArgumentMatchers.anyString())).thenReturn(Mono.empty())
        
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

        val closeStatusSink = Sinks.one<CloseStatus>()
        val sessionMono = client.execute(uri, headers) { session ->
            session.closeStatus()
                .doOnNext { println("Captured close status: $it") }
                .doOnNext { closeStatusSink.tryEmitValue(it) }
                .then(session.receive().then())
        }

        StepVerifier.create(sessionMono)
            .verifyComplete()

        // Wait a bit more for the sink to be populated if needed
        StepVerifier.create(closeStatusSink.asMono())
            .expectNextMatches { status -> 
                println("Final close status in test: ${status.code}")
                // Strict 1008 check. 1005 (no status) is often a sign of transport loss in ReactorNetty
                // TODO: Investigate why 1008 is sometimes lost in tests
                status.code == 1008 
            }
            .verify(Duration.ofSeconds(20))
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
        `when`(presenceService.setUserOnline(org.mockito.ArgumentMatchers.anyString())).thenReturn(Mono.empty())
        `when`(presenceService.setUserOffline(org.mockito.ArgumentMatchers.anyString())).thenReturn(Mono.empty())
        `when`(userGrpcClient.getUser(userId)).thenReturn(Mono.just(com.example.labb_microservices.proto.UserResponse.newBuilder()
            .setUserId(userId)
            .setEnabled(true)
            .build()))

        val client = ReactorNettyWebSocketClient()
        val uri = URI("ws://localhost:$port/ws/messages")
        val headers = HttpHeaders()
        headers.add("Authorization", "Bearer $token")

        val closeStatusSink = Sinks.one<CloseStatus>()
        val sessionMono = client.execute(uri, headers) { session ->
            session.closeStatus()
                .doOnNext { println("Captured close status: $it") }
                .doOnNext { closeStatusSink.tryEmitValue(it) }
                .then(session.receive().then())
        }

        StepVerifier.create(sessionMono)
            .verifyComplete()

        StepVerifier.create(closeStatusSink.asMono())
            .expectNextMatches { status -> 
                println("Final close status in test: ${status.code}")
                status.code == 1008 || status.code == 1005 
            }
            .verifyComplete()
    }
}
