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
    private val YOUTUBE_PREFERENCE = mapOf(
        "leutin09" to "UCl796X5XAJURXG2uvqR_XpQ",
        "adeptus ridiculous" to "UC8Xy_B9n35S5L695p9_2N_A",
        "last week tonight" to "UC3XTzVzaHQEd30qQbuWUctw",
        "john oliver" to "UC3XTzVzaHQEd30qQbuWUctw",
        "mrbeast" to "UCX6OQ3DkcsbYNE6H8uQQuVA",
        "marques brownlee" to "UCBJycsmduvYEL83R_U4JriQ",
        "mkbhd" to "UCBJycsmduvYEL83R_U4JriQ",
        "linus tech tips" to "UCXuqSBlHAE6Xw-yeJA0Tunw",
        "shroud" to "UCoz3Kpu5lv-ALhR4h9QN34g"
    )
    
    private val TWITCH_PREFERENCE = setOf(
        "shroud", "ninja", "pokimane", "lirik", "kaicenat", "xqc", "asmongold",
        "summit1g", "tyler1", "quin69", "forsen", "piratesoftware"
    )

    fun reasonAboutEntity(entityType: String, entityValue: String): EntityReasoning {
        val normalizedValue = entityValue.lowercase().trim()
        
        // 1. Check YouTube list
        val youtubeChannelId = YOUTUBE_PREFERENCE.entries.find { normalizedValue.contains(it.key) }?.value
        if (youtubeChannelId != null) {
            logger.info("Librarian identified $entityValue as a YouTube-centric entity.")
            return EntityReasoning(ContentSource.YOUTUBE, listOf(ContentSource.NEWS), platformHint = youtubeChannelId)
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
