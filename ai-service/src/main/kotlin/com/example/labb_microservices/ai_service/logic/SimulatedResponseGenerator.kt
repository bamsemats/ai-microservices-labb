package com.example.labb_microservices.ai_service.logic

import com.example.labb_microservices.ai_service.model.Message
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.time.Duration
import java.util.*

@Service
class SimulatedResponseGenerator : ResponseGenerator {
    private val logger = LoggerFactory.getLogger(SimulatedResponseGenerator::class.java)

    private val responses = listOf(
        "That's an interesting perspective! How does that make you feel about the overall project?",
        "I see where you're coming from. Have you considered the architectural trade-offs of that approach?",
        "Fascinating! The distributed nature of this system definitely makes that a challenge.",
        "I'm processing that... It seems like a classic microservices problem.",
        "Good point! We should probably look at the RabbitMQ metrics for that.",
        "I've analyzed the sentiment and it seems we're on the right track!"
    )

    override fun generateResponse(message: Message): Mono<String> {
        logger.info("Simulating LLM processing for message from: {}", message.senderId)
        
        // Simulate LLM latency (1-3 seconds)
        val latency = Random().nextLong(1000, 3000)
        
        return Mono.just(responses.random())
            .delayElement(Duration.ofMillis(latency))
            .doOnSuccess { logger.info("LLM simulation complete") }
    }
}
