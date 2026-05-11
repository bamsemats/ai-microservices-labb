package com.example.labb_microservices.ai_service.logic

import org.springframework.stereotype.Component

@Component
class BotRegistry {
    private val botMetadata = mapOf(
        "NexusPrime" to "NexusPrime (Architect)",
        "AdaptaAI" to "AdaptaAI (Assistant)",
        "EchoFlow" to "EchoFlow (Curator)",
        "VibeCheck" to "VibeCheck (Moderator)",
        "HelpDesk" to "Support (HelpDesk)"
    )

    fun isAiBot(id: String): Boolean = id in botMetadata || id == "ai-bot"

    fun getBotDisplayName(id: String): String {
        return botMetadata[id] ?: "AdaptaChat AI"
    }

    fun getBotId(id: String): String {
        return if (isAiBot(id)) id else "ai-bot"
    }
    
    fun getAllBotIds(): Set<String> = botMetadata.keys + "ai-bot"
}
