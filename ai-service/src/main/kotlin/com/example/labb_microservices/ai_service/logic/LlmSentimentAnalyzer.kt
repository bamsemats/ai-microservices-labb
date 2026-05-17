package com.example.labb_microservices.ai_service.logic

import com.example.labb_microservices.ai_service.model.AdaptationEvent
import com.example.labb_microservices.ai_service.model.OpenRouterMessage
import com.example.labb_microservices.ai_service.model.OpenRouterRequest
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Primary
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.util.concurrent.TimeUnit

@Primary

@Service
class LlmSentimentAnalyzer(


    private val webClientBuilder: WebClient.Builder,
    private val objectMapper: ObjectMapper,
    @Value("\${openrouter.api.key}") private val apiKey: String,
    @Value("\${openrouter.url}") private val url: String
) : SentimentAnalyzer {
    private val logger = LoggerFactory.getLogger(LlmSentimentAnalyzer::class.java)
    private val webClient = webClientBuilder.build()

    // Typed response for LLM output
    data class SentimentResponse(
        val theme: String = "neutral",
        val intensity: Double = 0.5,
        val color: String? = null,
        val blurAmount: Double? = null,
        val glassOpacity: Double? = null,
        val glowIntensity: Double? = null
    )
    
    // Using a reliable model for sentiment analysis
    private val sentimentModel = "openrouter/auto"

    override fun analyzeSentiment(content: String): Mono<AdaptationEvent?> {
        if (apiKey.isBlank() || apiKey == "\${OPENROUTER_API_KEY}") {
            logger.debug("OpenRouter API key missing for sentiment analysis. Using simulated sentiment.")
            return generateSimulatedSentiment(content)
        }

        val systemPrompt = """
            Analyze the tone of the chat message. Categorize it into one of these themes:
            - 'emergency': urgent, critical, danger, panic, help needed.
            - 'vibrant': happy, excited, positive, energetic, celebration.
            - 'zen': calm, peaceful, relaxed, meditative, quiet.
            - 'deep': focused, working, studying, technical, serious.
            - 'neutral': standard, casual, no strong emotion.

            Return ONLY a JSON object with these fields:
            {
              "theme": "theme_name",
              "intensity": 0.0 to 1.0,
              "color": "HEX_COLOR",
              "blurAmount": 0.0 to 30.0,
              "glassOpacity": 0.0 to 0.2,
              "glowIntensity": 0.0 to 1.0
            }
            
            Theme colors guidance:
            - emergency: #f43f5e
            - vibrant: #ec4899
            - zen: #06b6d4
            - deep: #8b5cf6
            - neutral: #6366f1
        """.trimIndent()

        val request = OpenRouterRequest(
            model = sentimentModel,
            messages = listOf(
                OpenRouterMessage(role = "system", content = systemPrompt),
                OpenRouterMessage(role = "user", content = content)
            ),
            stream = false
        )

        return webClient.post()
            .uri(url)
            .header("Authorization", "Bearer $apiKey")
            .header("X-Title", "AdaptaChat-Sentiment")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .retrieve()
            .bodyToMono(String::class.java)
            .timeout(java.time.Duration.ofSeconds(10))
            .flatMap { json ->
                try {
                    val root = objectMapper.readTree(json)
                    val contentResponse = root.path("choices").get(0).path("message").path("content").asText()
                    
                    // Robust JSON extraction
                    val jsonMatch = extractJson(contentResponse)
                    if (jsonMatch.isEmpty()) return@flatMap Mono.empty<AdaptationEvent>()

                    val response = objectMapper.readValue(jsonMatch, SentimentResponse::class.java)
                    
                    val allowedThemes = setOf("emergency", "vibrant", "zen", "deep")
                    val theme = response.theme.lowercase()
                    if (theme == "neutral") return@flatMap Mono.empty<AdaptationEvent>()
                    if (theme !in allowedThemes) {
                        logger.warn("Received unknown theme from LLM: {}", theme)
                        return@flatMap Mono.empty<AdaptationEvent>()
                    }

                    // Normalize and clamp properties
                    val intensity = (response.intensity).coerceIn(0.0, 1.0)
                    val blurAmount = (response.blurAmount ?: 0.0).coerceIn(0.0, 30.0)
                    val glassOpacity = (response.glassOpacity ?: 0.0).coerceIn(0.0, 0.2)
                    val glowIntensity = (response.glowIntensity ?: 0.0).coerceIn(0.0, 1.0)
                    
                    // Basic hex color validation
                    val color = response.color?.takeIf { it.matches(Regex("^#[0-9A-Fa-f]{6}$")) }
                    
                    Mono.just(AdaptationEvent(
                        theme = theme,
                        intensity = intensity,
                        color = color,
                        blurAmount = blurAmount,
                        glassOpacity = glassOpacity,
                        glowIntensity = glowIntensity
                    ))
                } catch (e: Exception) {
                    val sanitizedSnippet = if (json.length > 100) json.substring(0, 100) + "..." else json
                    logger.warn("Failed to parse semantic sentiment JSON snippet: {} (error: {})", sanitizedSnippet, e.message)
                    Mono.empty<AdaptationEvent>()
                }
            }
            .onErrorResume { e ->
                if (e is org.springframework.web.reactive.function.client.WebClientResponseException) {
                    if (e.statusCode == org.springframework.http.HttpStatus.UNAUTHORIZED || e.statusCode == org.springframework.http.HttpStatus.FORBIDDEN) {
                        logger.error("Error calling semantic sentiment analysis: {} - URL: {}", e.statusCode, url, e)
                        return@onErrorResume Mono.defer { generateSimulatedSentiment(content) }
                    }
                    logger.error("Error calling semantic sentiment analysis: {} - URL: {}", e.statusCode, url, e)
                } else {
                    logger.error("Error calling semantic sentiment analysis: {}", e.message, e)
                }
                Mono.empty()
            }
    }

    private fun generateSimulatedSentiment(content: String): Mono<AdaptationEvent?> {
        val lower = content.lowercase()
        val theme = when {
            lower.contains("help") || lower.contains("danger") || lower.contains("error") -> "emergency"
            lower.contains("yay") || lower.contains("celebrate") || lower.contains("party") -> "vibrant"
            lower.contains("peace") || lower.contains("calm") || lower.contains("relax") -> "zen"
            lower.contains("code") || lower.contains("technical") || lower.contains("study") -> "deep"
            else -> "neutral"
        }
        
        if (theme == "neutral") return Mono.empty()
        
        return Mono.just(AdaptationEvent(
            theme = theme,
            intensity = 0.5,
            color = when(theme) {
                "emergency" -> "#f43f5e"
                "vibrant" -> "#ec4899"
                "zen" -> "#06b6d4"
                "deep" -> "#8b5cf6"
                else -> "#6366f1"
            }
        ))
    }

    private fun extractJson(content: String): String {
        // 1. Try to find fenced code blocks
        val fencedMatch = Regex("```json\\s*([\\s\\S]*?)\\s*```").find(content)
        if (fencedMatch != null) return fencedMatch.groupValues[1].trim()

        // 2. Fallback to balanced braces
        var braceCount = 0
        var startIndex = -1
        for (i in content.indices) {
            if (content[i] == '{') {
                if (startIndex == -1) startIndex = i
                braceCount++
            } else if (content[i] == '}') {
                braceCount--
                if (braceCount == 0 && startIndex != -1) {
                    return content.substring(startIndex, i + 1)
                }
            }
        }

        return ""
    }
}
