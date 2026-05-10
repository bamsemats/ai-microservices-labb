package com.example.labb_microservices.message_service.model

import java.io.Serializable

data class TypingEvent(
    val type: String = "TYPING",
    val userId: String,
    val username: String? = null,
    val channelId: String,
    val isTyping: Boolean
) : Serializable
