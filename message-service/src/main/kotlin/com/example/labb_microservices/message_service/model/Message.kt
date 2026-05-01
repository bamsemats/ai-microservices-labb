package com.example.labb_microservices.message_service.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.io.Serializable
import java.time.LocalDateTime

@Document(collection = "messages")
data class Message(
    @Id
    val id: String? = null,
    val senderId: String,
    val receiverId: String,
    val content: String,
    val timestamp: LocalDateTime = LocalDateTime.now()
) : Serializable
