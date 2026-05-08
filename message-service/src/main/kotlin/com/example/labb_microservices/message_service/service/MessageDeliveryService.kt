package com.example.labb_microservices.message_service.service

import com.example.labb_microservices.message_service.session.SessionRegistry
import org.springframework.stereotype.Service

@Service
class MessageDeliveryService(
    private val sessionRegistry: SessionRegistry
) {
    fun broadcastMessage(message: String) {
        sessionRegistry.getAllSessions().forEach { it.sink.tryEmitNext(message) }
    }

    fun broadcastToChannel(channelId: String, message: String) {
        sessionRegistry.getAllSessions().forEach { session ->
            if (session.channelId == channelId || channelId == "all") {
                session.sink.tryEmitNext(message)
            }
        }
    }

    fun sendMessageToUser(userId: String, channelId: String, message: String) {
        sessionRegistry.getSessionsForUser(userId).forEach { session ->
            if (session.channelId == channelId || channelId == "all") {
                session.sink.tryEmitNext(message)
            }
        }
    }
}
