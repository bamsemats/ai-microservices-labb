package com.example.labb_microservices.ai_service.logic

import com.example.labb_microservices.ai_service.config.BotProperties
import org.springframework.stereotype.Component

@Component
class BotRegistry(private val botProperties: BotProperties) {
    private val botMetadata: Map<String, String> by lazy {
        botProperties.metadata.mapKeys { it.key.lowercase() }
    }

    fun isAiBot(id: String): Boolean {
        val lid = id.lowercase()
        return lid in botMetadata || lid == "ai-bot" || lid == "ai"
    }

    fun getBotDisplayName(id: String): String {
        return botMetadata[id.lowercase()] ?: "AdaptaChat AI"
    }

    fun getBotId(id: String): String {
        val lid = id.lowercase()
        return if (isAiBot(lid)) {
            // Return the canonical ID if it exists in metadata, otherwise the original
            botMetadata.keys.find { it == lid } ?: lid
        } else {
            "ai-bot"
        }
    }
    
    fun getAllBotIds(): Set<String> = botMetadata.keys + "ai-bot" + "ai"
}
