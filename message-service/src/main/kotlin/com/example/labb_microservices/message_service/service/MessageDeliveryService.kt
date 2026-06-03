package com.example.labb_microservices.message_service.service

import com.example.labb_microservices.message_service.session.SessionRegistry
import io.micrometer.observation.annotation.Observed
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Sinks

@Service
@Observed(name = "message.delivery")
class MessageDeliveryService(
    private val sessionRegistry: SessionRegistry
) {
    private val logger = LoggerFactory.getLogger(MessageDeliveryService::class.java)

    fun broadcastMessage(message: String) {
        sessionRegistry.getAllSessions().forEach { session ->
            val result = session.sink.tryEmitNext(message)
            if (result.isFailure) {
                logger.warn("Failed to emit broadcast message to session {}: {}", session.sessionId, result)
            }
        }
    }

    fun broadcastToChannel(channelId: String, message: String) {
        // If it's a global/public channel, broadcast to ALL sessions to ensure delivery
        // regardless of which specific channel the session started with.
        if (channelId == "general" || channelId == "global" || channelId == "all") {
            broadcastMessage(message)
            return
        }

        val targetSessions = sessionRegistry.getSessionsForChannel(channelId)

        targetSessions.forEach { session ->
            val result = session.sink.tryEmitNext(message)
            if (result.isFailure) {
                logger.warn("Failed to emit channel message to session {} in channel {}: {}", session.sessionId, channelId, result)
            }
        }
    }

    fun sendMessageToUser(userId: String, message: String) {
        sessionRegistry.getSessionsForUser(userId).forEach { session ->
            val result = session.sink.tryEmitNext(message)
            if (result.isFailure) {
                logger.warn("Failed to emit private message to user {} session {}: {}", userId, session.sessionId, result)
            }
        }
    }
}
