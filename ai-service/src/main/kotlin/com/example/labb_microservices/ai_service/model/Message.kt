package com.example.labb_microservices.ai_service.model

import java.io.Serializable
import java.time.LocalDateTime

enum class AuthorType { USER, BOT }

data class Message(
    val id: String? = null,
    val senderId: String,
    val receiverId: String,
    val content: String,
    val authorType: AuthorType = AuthorType.USER,
    val timestamp: LocalDateTime = LocalDateTime.now()
) : Serializable
