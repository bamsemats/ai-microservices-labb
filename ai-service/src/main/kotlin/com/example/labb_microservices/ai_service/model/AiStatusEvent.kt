package com.example.labb_microservices.ai_service.model

data class AiStatusEvent(
    val type: String = "AI_STATUS",
    val status: String, // "THINKING", "COMPLETED", "ERROR"
    val channelId: String,
    val userId: String? = null
)
