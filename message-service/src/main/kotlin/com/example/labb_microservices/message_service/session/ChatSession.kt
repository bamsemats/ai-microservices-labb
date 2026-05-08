package com.example.labb_microservices.message_service.session

import reactor.core.publisher.Sinks

data class ChatSession(
    val sessionId: String,
    var userId: String? = null,
    val channelId: String,
    var token: String? = null,
    val sink: Sinks.Many<String>,
    val disconnectSink: Sinks.Empty<Void>
)
