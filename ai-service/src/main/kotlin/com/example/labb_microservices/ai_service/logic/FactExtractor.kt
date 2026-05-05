package com.example.labb_microservices.ai_service.logic

import com.example.labb_microservices.ai_service.model.MemoryCategory
import com.example.labb_microservices.ai_service.model.Message
import reactor.core.publisher.Flux

data class ExtractedFact(
    val category: MemoryCategory,
    val value: String,
    val confidence: Double
)

interface FactExtractor {
    fun extractFacts(message: Message): Flux<ExtractedFact>
}
