package com.example.labb_microservices.content_aggregator.model

import java.io.Serializable

enum class AuthorType { USER, BOT }

data class Message(
    val id: String? = null,
    val senderId: String,
    val receiverId: String,
    val channelId: String = "general",
    val content: String,
    val authorType: AuthorType = AuthorType.USER
) : Serializable
