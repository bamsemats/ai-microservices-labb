package com.example.labb_microservices.ai_service.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "ai.bots")
data class BotProperties(
    val metadata: Map<String, String> = emptyMap()
)
