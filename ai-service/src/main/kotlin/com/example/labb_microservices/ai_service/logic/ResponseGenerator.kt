package com.example.labb_microservices.ai_service.logic

import com.example.labb_microservices.ai_service.model.Message
import reactor.core.publisher.Mono

interface ResponseGenerator {
    fun generateResponse(message: Message): Mono<String>
}
