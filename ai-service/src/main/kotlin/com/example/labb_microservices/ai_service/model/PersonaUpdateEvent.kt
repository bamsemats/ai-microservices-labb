package com.example.labb_microservices.ai_service.model

data class PersonaUpdateEvent(
    val userId: String,
    val category: MemoryCategory,
    val value: String,
    val confidence: Double
)
