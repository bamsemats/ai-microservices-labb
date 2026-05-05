package com.example.labb_microservices.ai_service.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

data class OpenRouterRequest(
    val model: String,
    val messages: List<OpenRouterMessage>,
    val stream: Boolean = false
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class OpenRouterMessage(
    val role: String,
    val content: String
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class OpenRouterResponse(
    val choices: List<OpenRouterChoice>
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class OpenRouterChoice(
    val message: OpenRouterMessage? = null,
    val delta: OpenRouterDelta? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class OpenRouterDelta(
    val content: String? = null
)
