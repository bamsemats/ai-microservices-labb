package com.example.labb_microservices.ai_service.logic

import com.example.labb_microservices.ai_service.model.Message
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Duration
import java.util.*

@Service
class SimulatedResponseGenerator(
    private val memoryFragmentRepository: com.example.labb_microservices.ai_service.repository.MemoryFragmentRepository
) : ResponseGenerator {
    private val logger = LoggerFactory.getLogger(SimulatedResponseGenerator::class.java)

    private val responses = listOf(
        "That's an interesting perspective! How does that make you feel about the overall project?",
        "I see where you're coming from. Have you considered the architectural trade-offs of that approach?",
        "Fascinating! The distributed nature of this system definitely makes that a challenge.",
        "I'm processing that... It seems like a classic microservices problem.",
        "Good point! We should probably look at the RabbitMQ metrics for that.",
        "I've analyzed the sentiment and it seems we're on the right track!"
    )

    override fun generateResponse(message: Message): Flux<String> {
        logger.info("Simulating LLM processing for message from: {}", message.senderId)
        
        return memoryFragmentRepository.findByUserId(message.senderId)
            .collectList()
            .flatMapMany { fragments ->
                val context = if (fragments.isNotEmpty()) {
                    "User Profile Context: " + fragments.joinToString(", ") { "${it.category}: ${it.value}" }
                } else {
                    "No profile context available."
                }
                logger.info("Context for AI: {}", context)

                val baseResponse = responses.random()
                val personalTouch = if (fragments.isNotEmpty()) {
                    val fragment = fragments.random()
                    val mention = when (fragment.category) {
                        com.example.labb_microservices.ai_service.model.MemoryCategory.TECH_STACK -> "Since you know ${fragment.value}, you might appreciate this."
                        com.example.labb_microservices.ai_service.model.MemoryCategory.INTEREST -> "Given your interest in ${fragment.value}, this is quite relevant."
                        com.example.labb_microservices.ai_service.model.MemoryCategory.GOAL -> "This aligns well with your goal to ${fragment.value}."
                        else -> "Since you're into ${fragment.value}, check this out."
                    }
                    " $mention"
                } else {
                    ""
                }

                val finalResponse = baseResponse + personalTouch
                
                // Simulate LLM latency and streaming chunks
                val words = finalResponse.split(" ")
                Flux.fromIterable(words)
                    .map { if (it == words.last()) it else "$it " }
                    .delayElements(Duration.ofMillis(100))
            }
            .doOnComplete { logger.info("LLM simulation complete") }
    }
}
