package com.example.labb_microservices.ai_service.logic

import com.example.labb_microservices.ai_service.config.BotProperties
import org.springframework.stereotype.Component

@Component
class BotRegistry(private val botProperties: BotProperties) {
    private val botMetadata: Map<String, String>
        get() = botProperties.metadata

    fun isAiBot(id: String): Boolean = id in botMetadata || id == "ai-bot"

    fun getBotDisplayName(id: String): String {
        return botMetadata[id] ?: "AdaptaChat AI"
    }

    fun getBotId(id: String): String {
        return if (isAiBot(id)) id else "ai-bot"
    }
    
    fun getAllBotIds(): Set<String> = botMetadata.keys + "ai-bot"
}
