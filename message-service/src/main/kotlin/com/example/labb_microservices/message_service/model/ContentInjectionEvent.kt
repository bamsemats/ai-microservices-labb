package com.example.labb_microservices.message_service.model

data class ContentInjectionEvent(
    val type: String = "CONTENT_INJECTION",
    val contentType: String,
    val data: Map<String, String>,
    val timestamp: Long = System.currentTimeMillis()
)
