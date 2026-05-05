package com.example.labb_microservices.ai_service.model

data class OpenRouterRequest(
    val model: String,
    val messages: List<OpenRouterMessage>,
    val stream: Boolean = false
)

data class OpenRouterMessage(
    val role: String,
    val content: String
)

data class OpenRouterResponse(
    val choices: List<OpenRouterChoice>
)

data class OpenRouterChoice(
    val message: OpenRouterMessage? = null,
    val delta: OpenRouterDelta? = null
)

data class OpenRouterDelta(
    val content: String? = null
)
