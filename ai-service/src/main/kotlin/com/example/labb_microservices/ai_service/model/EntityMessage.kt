package com.example.labb_microservices.ai_service.model

data class EntityMessage(
    val entityType: String,
    val entityValue: String,
    val originalMessageId: String,
    val senderId: String
)
