package com.example.labb_microservices.ai_service.model

data class AdaptationEvent(
    val type: String = "UI_ADAPTATION",
    val theme: String,
    val intensity: Double? = null,
    val color: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
