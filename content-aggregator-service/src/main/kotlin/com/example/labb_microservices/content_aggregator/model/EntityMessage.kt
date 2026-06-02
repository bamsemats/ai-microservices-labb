package com.example.labb_microservices.content_aggregator.model

/**
 * Shared Contract: This DTO matches the one in ai-service.
 * Decoupled intentionally to preserve Bounded Context.
 */
data class EntityMessage(
    val entityType: String,
    val entityValue: String,
    val originalMessageId: String,
    val channelId: String = "general",
    val senderId: String = "",
    val confidence: Double = 1.0
)
