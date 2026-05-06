package com.example.labb_microservices.message_service.model

data class AdaptationEvent(
    val type: String = "UI_ADAPTATION",
    val theme: String,
    val intensity: Double? = null,
    val color: String? = null,
    val primaryColor: String? = null,
    val blurAmount: Double? = null,
    val glassOpacity: Double? = null,
    val glowIntensity: Double? = null,
    val timestamp: Long = System.currentTimeMillis()
)
