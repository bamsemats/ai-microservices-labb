package com.example.labb_microservices.content_aggregator.model

data class ContentInjectionEvent(
    val type: String = "CONTENT_INJECTION",
    val contentType: String, // e.g., "TWITCH_STREAM"
    val data: Map<String, String>,
    val timestamp: Long = System.currentTimeMillis()
)
