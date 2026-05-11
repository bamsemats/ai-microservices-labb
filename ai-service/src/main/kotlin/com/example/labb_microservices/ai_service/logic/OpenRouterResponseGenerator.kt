package com.example.labb_microservices.ai_service.logic

import com.example.labb_microservices.ai_service.model.*
import com.example.labb_microservices.ai_service.repository.MemoryFragmentRepository
import com.fasterxml.jackson.databind.ObjectMapper
import io.micrometer.observation.annotation.Observed
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Primary
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.MediaType
import org.springframework.http.codec.ServerSentEvent
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Primary
@Service
@Observed(name = "ai.response.generation")
class OpenRouterResponseGenerator(
    private val memoryFragmentRepository: MemoryFragmentRepository,
    private val piiRedactor: PiiRedactor,
    private val webClientBuilder: WebClient.Builder,
    private val objectMapper: ObjectMapper,
    @Value("\${openrouter.api.key}") private val apiKey: String,
    @Value("\${openrouter.model}") private val model: String,
    @Value("\${openrouter.url}") private val url: String
) : ResponseGenerator {
    private val logger = LoggerFactory.getLogger(OpenRouterResponseGenerator::class.java)
    private val webClient = webClientBuilder.build()

    override fun generateResponse(message: Message): Flux<String> {
        // Support for Test Mode (X-Adapta-Test-Mode header propagated via metadata)
        if (message.metadata["X-Adapta-Test-Mode"] == "true") {
            logger.info("Test mode detected for message: {}. Returning mock response with context.", message.id)
            return memoryFragmentRepository.findByUserId(message.senderId)
                .collectList()
                .flatMapMany { fragments ->
                    val contextStr = fragments.joinToString(", ") { it.value }
                    Flux.just("Deterministic mock response. Context found: [$contextStr]")
                }
        }

        logger.info("Generating streaming real LLM response for user: {}", message.senderId)
        
        return memoryFragmentRepository.findByUserId(message.senderId)
            .collectList()
            .flatMapMany { fragments ->
                val context = if (fragments.isNotEmpty()) {
                    "User Context (Past Facts):\n" + fragments.joinToString("\n") { "- ${it.category}: ${piiRedactor.redact(it.value)}" }
                } else {
                    ""
                }

                val systemPrompt = """
                    You are a helpful AI assistant in a professional microservices chat system.
                    $context
                    
                    Instructions:
                    - Keep your responses professional and concise.
                    - If user context is provided, try to naturally reference it if relevant.
                    - Do not mention that you are an AI or use robotic phrases.
                    - If you see [EMAIL_REDACTED] or [PHONE_REDACTED], respect the privacy and do not try to guess the original values.
                """.trimIndent()

                val userContent = piiRedactor.redact(message.content)
                
                val request = OpenRouterRequest(
                    model = model,
                    messages = listOf(
                        OpenRouterMessage(role = "system", content = systemPrompt),
                        OpenRouterMessage(role = "user", content = userContent)
                    ),
                    stream = true
                )

                logger.info("Sending streaming request to OpenRouter using model: {} at URL: {}", model, url)

                if (apiKey.isBlank() || apiKey == "\${OPENROUTER_API_KEY}") {
                    logger.warn("OpenRouter API key is missing. Using simulation fallback.")
                    return@flatMapMany generateSimulatedResponse(message)
                }

                webClient.post()
                    .uri(url)
                    .header("Authorization", "Bearer $apiKey")
                    .header("HTTP-Referer", "http://localhost:3000")
                    .header("X-Title", "AdaptaChat")
                    .accept(MediaType.TEXT_EVENT_STREAM)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToFlux(object : ParameterizedTypeReference<ServerSentEvent<String>>() {})
                    .timeout(java.time.Duration.ofSeconds(30))
                    .mapNotNull { it.data() }
                    .filter { it != "[DONE]" }
                    .map { json ->
                        try {
                            val response = objectMapper.readValue(json, OpenRouterResponse::class.java)
                            response.choices.firstOrNull()?.delta?.content ?: ""
                        } catch (e: Exception) {
                            logger.warn("Failed to parse AI chunk: {}", json, e)
                            ""
                        }
                    }
                    .filter { it.isNotEmpty() }
                    .onErrorResume { e ->
                        if (e is org.springframework.web.reactive.function.client.WebClientResponseException.Unauthorized) {
                            logger.error("Unauthorized call to OpenRouter. Switching to simulation mode.")
                            generateSimulatedResponse(message)
                        } else if (e is java.util.concurrent.TimeoutException) {
                            logger.error("Timeout calling OpenRouter: {}", e.message)
                            Flux.just("I'm having trouble connecting to my brain right now. (Timeout)")
                        } else {
                            logger.error("Error calling OpenRouter: {}", e.message)
                            Flux.just("Interference detected in the frequency. Please try again later.")
                        }
                    }
            }
    }

    private fun generateSimulatedResponse(message: Message): Flux<String> {
        val content = message.content.lowercase()
        val response = when {
            content.contains("hello") || content.contains("hi") -> 
                "Greetings! I am the AdaptaChat AI. I am currently running in simulation mode because my external uplink is unauthorized or missing. How can I help you navigate these frequencies today?"
            content.contains("weather") -> 
                "The climate in the digital realm is steady, with a 100% chance of data packets. For physical weather, please consult a atmospheric sensor."
            content.contains("help") -> 
                "I can assist with navigating the Discovery Hub, understanding your Insights, or just chatting. Note: I am currently in 'Offline Mode' due to API key issues."
            else -> 
                "I've received your transmission: '${message.content}'. I'm currently operating in limited capacity, but I am standing by for your next synchronization."
        }
        
        // Split response into chunks to simulate streaming
        return Flux.fromIterable(response.split(" ").map { "$it " })
            .delayElements(java.time.Duration.ofMillis(100))
    }
}
