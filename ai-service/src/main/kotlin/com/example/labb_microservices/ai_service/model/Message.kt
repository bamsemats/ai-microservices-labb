package com.example.labb_microservices.ai_service.model

import com.fasterxml.jackson.annotation.JsonFormat
import java.io.Serializable
import java.time.LocalDateTime

enum class AuthorType { USER, BOT }

data class Message(
    val id: String? = null,
    val senderId: String,
    val senderName: String? = null,
    val receiverId: String,
    val channelId: String = "general",
    val content: String,
    val authorType: AuthorType = AuthorType.USER,
    val metadata: Map<String, String> = emptyMap(),
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val contentChunks: List<String> = emptyList(),
    val searchIndices: Set<String> = emptySet(),
    val readBy: Set<String> = emptySet()
) : Serializable
