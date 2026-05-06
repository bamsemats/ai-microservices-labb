package com.example.labb_microservices.message_service.handler

import com.example.common.test.BaseIntegrationTest
import com.example.labb_microservices.common.security.JwtTokenValidator
import com.example.labb_microservices.message_service.client.UserGrpcClient
import com.example.labb_microservices.message_service.service.PresenceService
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpHeaders
import org.springframework.web.reactive.socket.CloseStatus
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient
import reactor.core.publisher.Mono
import reactor.core.publisher.Sinks
import reactor.test.StepVerifier
import java.net.URI
import java.time.Duration
import org.springframework.beans.factory.annotation.Autowired
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertEquals

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = [
    "jwt.secret=a-very-long-and-secure-secret-key-that-is-at-least-256-bits",
    "encryption.secret=another-very-long-and-secure-secret-key-32-chars",
    "grpc.server.port=0",
    "auth.cache.ttl=0",
    "auth.validation.interval=1"
])
class MessageWebSocketSecurityTests : BaseIntegrationTest() {

    @Autowired
    private lateinit var webSocketHandler: MessageWebSocketHandler

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
            .expectComplete()
            .verify(Duration.ofSeconds(20))

        StepVerifier.create(closeStatusSink.asMono())
            .assertNext { status -> 
                println("Final close status in test: ${status.code}")
                // Side-channel check: verify the server signaled a violation
                assertTrue(webSocketHandler.policyViolations.get() > 0, "Server should have recorded a policy violation")
                
                // Strict 1008 check as per mandate. 
                // Note: ReactorNetty sometimes returns 1005 (Empty) if the transport closes abruptly.
                // TODO: Root-cause close code loss in ReactorNetty transport.
                assertEquals(1008, status.code, "Expected close code 1008 (Policy Violation)")
            }
            .expectComplete()
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
            .expectComplete()
            .verify(Duration.ofSeconds(20))

        StepVerifier.create(closeStatusSink.asMono())
            .assertNext { status -> 
                println("Final close status in test: ${status.code}")
                // Side-channel check: verify the server signaled a violation
                assertTrue(webSocketHandler.policyViolations.get() > 0, "Server should have recorded a policy violation")
                
                // Strict 1008 check as per mandate. 
                // Note: ReactorNetty sometimes returns 1005 (Empty) if the transport closes abruptly.
                // TODO: Root-cause close code loss in ReactorNetty transport.
                assertEquals(1008, status.code, "Expected close code 1008 (Policy Violation)")
            }
            .expectComplete()
            .verify(Duration.ofSeconds(20))
    }
}
