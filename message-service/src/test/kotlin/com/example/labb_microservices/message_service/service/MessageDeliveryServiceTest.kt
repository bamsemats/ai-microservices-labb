package com.example.labb_microservices.message_service.service

import com.example.labb_microservices.message_service.session.ChatSession
import com.example.labb_microservices.message_service.session.SessionRegistry
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import reactor.core.publisher.Sinks

class MessageDeliveryServiceTest {

    @Test
    fun `sendMessageToUser should deliver message regardless of session channelId`() {
        val sessionRegistry = mock(SessionRegistry::class.java)
        val deliveryService = MessageDeliveryService(sessionRegistry)
        
        val userId = "user-123"
        val sessionId = "session-abc"
        val sink = Sinks.many().multicast().onBackpressureBuffer<String>()
        val session = ChatSession(
            sessionId = sessionId,
            userId = userId,
            channelId = "different-channel",
            sink = sink,
            disconnectSink = Sinks.empty()
        )

        `when`(sessionRegistry.getSessionsForUser(userId)).thenReturn(listOf(session))

        val message = "Hello World"
        
        // Start verification before emitting
        val verifier = reactor.test.StepVerifier.create(sink.asFlux())
            .then {
                deliveryService.sendMessageToUser(userId, message)
            }
            .expectNext(message)
            .thenCancel()
            .verify(java.time.Duration.ofSeconds(5))
    }
}
