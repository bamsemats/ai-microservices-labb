package com.example.labb_microservices.ai_service.logic

import com.example.labb_microservices.ai_service.model.Message
import reactor.core.publisher.Flux

interface ResponseGenerator {
    fun generateResponse(message: Message): Flux<String>
}
