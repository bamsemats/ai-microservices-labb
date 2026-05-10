package com.example.labb_microservices.message_service.service

import com.example.labb_microservices.message_service.session.SessionRegistry
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Sinks

@Service
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
        val targetSessions = if (channelId == "all") {
            sessionRegistry.getAllSessions()
        } else {
            sessionRegistry.getSessionsForChannel(channelId)
        }

        targetSessions.forEach { session ->
            val result = session.sink.tryEmitNext(message)
            if (result.isFailure) {
                logger.warn("Failed to emit channel message to session {} in channel {}: {}", session.sessionId, channelId, result)
            }
        }
    }

    fun sendMessageToUser(userId: String, channelId: String, message: String) {
        sessionRegistry.getSessionsForUser(userId).forEach { session ->
            if (session.channelId == channelId || channelId == "all") {
                val result = session.sink.tryEmitNext(message)
                if (result.isFailure) {
                    logger.warn("Failed to emit private message to user {} session {}: {}", userId, session.sessionId, result)
                }
            }
        }
    }
}
