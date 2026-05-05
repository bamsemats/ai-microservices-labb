package com.example.labb_microservices.ai_service.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

enum class MemoryCategory {
    INTEREST,
    TECH_STACK,
    PERSONALITY_TRAIT,
    GOAL
}

@Document(collection = "memory_fragments")
data class MemoryFragment(
    @Id
    val id: String? = null,
    @Indexed
    val userId: String,
    val category: MemoryCategory,
    val value: String,
    val confidence: Double,
    val sourceMessageId: String,
    val timestamp: Instant = Instant.now()
)
