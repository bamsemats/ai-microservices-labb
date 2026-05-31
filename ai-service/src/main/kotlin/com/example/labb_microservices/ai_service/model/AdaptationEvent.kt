package com.example.labb_microservices.ai_service.model

data class AdaptationEvent(
    val theme: String,
    val intensity: Double,
    val color: String? = null,
    val blurAmount: Double? = null,
    val glassOpacity: Double? = null,
    val glowIntensity: Double? = null,
    val entities: List<EntityMessage>? = null,
    val messageId: String? = null
)
