package com.example.labb_microservices.ai_service.logic

import com.example.labb_microservices.ai_service.config.BotProperties
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BotRegistryTest {

    @Test
    fun `should identify AI bots from properties`() {
        val properties = BotProperties(metadata = mapOf("TestBot" to "Test Bot Name"))
        val registry = BotRegistry(properties)

        assertTrue(registry.isAiBot("TestBot"))
        assertTrue(registry.isAiBot("ai-bot"))
    }

    @Test
    fun `should return display name from properties`() {
        val properties = BotProperties(metadata = mapOf("TestBot" to "Test Bot Name"))
        val registry = BotRegistry(properties)

        assertEquals("Test Bot Name", registry.getBotDisplayName("TestBot"))
        assertEquals("AdaptaChat AI", registry.getBotDisplayName("UnknownBot"))
    }
}
