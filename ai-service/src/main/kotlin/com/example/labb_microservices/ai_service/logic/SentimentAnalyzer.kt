package com.example.labb_microservices.ai_service.logic

import com.example.labb_microservices.ai_service.model.AdaptationEvent
import reactor.core.publisher.Mono

interface SentimentAnalyzer {
    fun analyzeSentiment(content: String): Mono<AdaptationEvent?>
}
