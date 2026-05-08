package com.example.labb_microservices.ai_service.logic

import com.example.labb_microservices.ai_service.model.MemoryCategory
import com.example.labb_microservices.ai_service.model.Message
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux

@Service
class SimulatedFactExtractor : FactExtractor {
    
    override fun extractFacts(message: Message): Flux<ExtractedFact> {
        val content = message.content.lowercase()
        val facts = mutableListOf<ExtractedFact>()

        val boost = when (message.metadata["X-Confidence-Boost"]) {
            "true" -> 0.05
            "false" -> -0.10
            else -> 0.0
        }

        // Tech Stack
        if (content.contains("react")) facts.add(ExtractedFact(MemoryCategory.TECH_STACK, "React", (0.95 + boost).coerceIn(0.0, 1.0)))
        if (content.contains("kotlin")) facts.add(ExtractedFact(MemoryCategory.TECH_STACK, "Kotlin", (0.95 + boost).coerceIn(0.0, 1.0)))
        if (content.contains("spring boot")) facts.add(ExtractedFact(MemoryCategory.TECH_STACK, "Spring Boot", (0.9 + boost).coerceIn(0.0, 1.0)))
        if (content.contains("mongodb")) facts.add(ExtractedFact(MemoryCategory.TECH_STACK, "MongoDB", (0.85 + boost).coerceIn(0.0, 1.0)))

        // Interests
        if (content.contains("elden ring")) facts.add(ExtractedFact(MemoryCategory.INTEREST, "Elden Ring", 0.9))
        if (content.contains("valorant")) facts.add(ExtractedFact(MemoryCategory.INTEREST, "Valorant", 0.9))
        if (content.contains("music")) facts.add(ExtractedFact(MemoryCategory.INTEREST, "Music", 0.7))
        
        val interestMatch = Regex("i love\\b\\s*([^.!?]+)", RegexOption.IGNORE_CASE).find(message.content)
        if (interestMatch != null) {
            val thing = interestMatch.groupValues[1].trim()
            if (thing.length > 2) {
                val formattedThing = thing.split(" ").joinToString(" ") { it.replaceFirstChar { char -> if (char.isLowerCase()) char.titlecase() else char.toString() } }
                facts.add(ExtractedFact(MemoryCategory.INTEREST, formattedThing, 0.6))
            }
        }

        // Goals
        val goalMatch = Regex("(?:want to learn|learning)\\b\\s*([^.!?]+)", RegexOption.IGNORE_CASE).find(message.content)
        if (goalMatch != null) {
            val topic = goalMatch.groupValues[1].trim()
            if (topic.length > 2) {
                val formattedTopic = topic.split(" ").joinToString(" ") { it.replaceFirstChar { char -> if (char.isLowerCase()) char.titlecase() else char.toString() } }
                facts.add(ExtractedFact(MemoryCategory.GOAL, "Learn $formattedTopic", 0.8))
            }
        }

        return Flux.fromIterable(facts)
    }
}
