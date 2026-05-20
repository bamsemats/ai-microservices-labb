package com.example.labb_microservices.message_service.model

import com.fasterxml.jackson.annotation.JsonFormat
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.io.Serializable
import java.time.Instant

enum class AuthorType { USER, BOT }

@Document(collection = "messages")
data class Message(
    @Id
    val id: String? = null,
    val senderId: String,
    val senderName: String? = null,
    val receiverId: String,
    val channelId: String = "general",
    val content: String,
    val authorType: AuthorType = AuthorType.USER,
    val metadata: Map<String, String> = emptyMap(),
    val timestamp: Instant = Instant.now(),
    val contentChunks: List<String> = emptyList(),
    val searchIndices: Set<String> = emptySet(),
    val readBy: Set<String> = emptySet()
) : Serializable
