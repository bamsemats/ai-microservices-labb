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
    
    // Using a fast, small model for sentiment analysis
    private val sentimentModel = "mistralai/mistral-7b-instruct:free"

    override fun analyzeSentiment(content: String): Mono<AdaptationEvent?> {
        if (apiKey.isBlank() || apiKey == "\${OPENROUTER_API_KEY}") {
            return Mono.empty()
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
            .map { json ->
                try {
                    val root = objectMapper.readTree(json)
                    val contentResponse = root.path("choices").get(0).path("message").path("content").asText()
                    
                    // Robust JSON extraction
                    val jsonMatch = extractJson(contentResponse)
                    if (jsonMatch.isEmpty()) return@map null

                    val response = objectMapper.readValue(jsonMatch, SentimentResponse::class.java)
                    
                    if (response.theme == "neutral") return@map null
                    
                    AdaptationEvent(
                        theme = response.theme,
                        intensity = response.intensity,
                        color = response.color,
                        blurAmount = response.blurAmount,
                        glassOpacity = response.glassOpacity,
                        glowIntensity = response.glowIntensity
                    )
                } catch (e: Exception) {
                    logger.warn("Failed to parse semantic sentiment JSON: {}", json, e)
                    null
                }
            }
            .onErrorResume { e ->
                logger.error("Error calling semantic sentiment analysis: {}", e.message)
                Mono.empty()
            }
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
