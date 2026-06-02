package com.example.labb_microservices.ai_service.logic

import com.example.labb_microservices.ai_service.model.AdaptationEvent
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class SimulatedSentimentAnalyzer : SentimentAnalyzer {
    override fun analyzeSentiment(content: String): Mono<AdaptationEvent?> {
        val lower = content.lowercase()
        val event = when {
            Regex("\\b(urgent|critical|help|emergency|911)\\b").containsMatchIn(lower) -> 
                AdaptationEvent(theme = "emergency", intensity = 0.9, color = "#f43f5e", blurAmount = 24.0, glassOpacity = 0.15)
            Regex("\\b(happy|great|awesome|joy|excited)\\b").containsMatchIn(lower) -> 
                AdaptationEvent(theme = "vibrant", intensity = 0.8, color = "#ec4899", blurAmount = 16.0, glassOpacity = 0.08)
            Regex("\\b(calm|relax|sleep|peaceful|zen)\\b").containsMatchIn(lower) -> 
                AdaptationEvent(theme = "zen", intensity = 0.2, color = "#06b6d4", blurAmount = 8.0, glassOpacity = 0.02)
            Regex("\\b(focus|work|study|job|deep)\\b").containsMatchIn(lower) -> 
                AdaptationEvent(theme = "deep", intensity = 0.4, color = "#8b5cf6", blurAmount = 20.0, glassOpacity = 0.12)
            else -> null
        }
        return Mono.justOrEmpty(event)
    }
}
