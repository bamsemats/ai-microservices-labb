package com.example.labb_microservices.content_aggregator.model

data class EntityMessage(
    val entityType: String,
    val entityValue: String,
    val originalMessageId: String,
    val senderId: String
)
