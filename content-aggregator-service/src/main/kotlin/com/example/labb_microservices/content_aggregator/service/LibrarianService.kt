package com.example.labb_microservices.content_aggregator.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

enum class ContentSource {
    TWITCH, YOUTUBE, NEWS, FORUM, SOCIAL
}

data class EntityReasoning(
    val primarySource: ContentSource,
    val alternativeSources: List<ContentSource> = emptyList(),
    val confidenceAdjustment: Double = 1.0,
    val platformHint: String? = null
)

@Service
class LibrarianService {
    private val logger = LoggerFactory.getLogger(LibrarianService::class.java)

    // Known entities and their preferred platforms to reduce hallucinations
    private val YOUTUBE_PREFERENCE = setOf(
        "leutin09", "adeptus ridiculous", "wolflord", "major kill", "weshammer", 
        "baldermort", "oculus imperia", "taktical", "iswear"
    )
    
    private val TWITCH_PREFERENCE = setOf(
        "shroud", "ninja", "pokimane", "lirik", "kaicenat", "xqc", "asmongold",
        "summit1g", "tyler1", "quin69", "forsen"
    )

    fun reasonAboutEntity(entityType: String, entityValue: String): EntityReasoning {
        val normalizedValue = entityValue.lowercase().trim()
        
        // 1. Check YouTube list
        if (YOUTUBE_PREFERENCE.any { normalizedValue.contains(it) }) {
            logger.info("Librarian identified $entityValue as a YouTube-centric entity.")
            return EntityReasoning(ContentSource.YOUTUBE, listOf(ContentSource.NEWS))
        }

        // 2. Check Twitch list
        if (TWITCH_PREFERENCE.any { normalizedValue.contains(it) }) {
            logger.info("Librarian identified $entityValue as a Twitch-centric entity.")
            return EntityReasoning(ContentSource.TWITCH, listOf(ContentSource.SOCIAL))
        }

        // 3. Fallback based on AI-detected type
        return when (entityType.uppercase()) {
            "STREAMER" -> EntityReasoning(ContentSource.TWITCH, listOf(ContentSource.YOUTUBE))
            "VIDEO" -> EntityReasoning(ContentSource.YOUTUBE)
            "GAME" -> EntityReasoning(ContentSource.TWITCH, listOf(ContentSource.NEWS, ContentSource.FORUM))
            "NEWS" -> EntityReasoning(ContentSource.NEWS)
            "SOCIAL" -> EntityReasoning(ContentSource.SOCIAL)
            "FORUM" -> EntityReasoning(ContentSource.FORUM)
            else -> EntityReasoning(ContentSource.NEWS, listOf(ContentSource.FORUM))
        }
    }
}
