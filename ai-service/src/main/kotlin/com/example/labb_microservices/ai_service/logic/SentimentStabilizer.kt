package com.example.labb_microservices.ai_service.logic

import com.example.labb_microservices.ai_service.model.AdaptationEvent
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

@Component
class SentimentStabilizer {
    private val logger = LoggerFactory.getLogger(SentimentStabilizer::class.java)
    
    // Track channel state: ChannelId -> LastEvent
    private val lastEvents = ConcurrentHashMap<String, AdaptationEvent>()
    
    // Grace period in milliseconds (e.g., 10 seconds)
    private val gracePeriodMs = 10000L

    fun shouldPublish(channelId: String, event: AdaptationEvent): Boolean {
        val lastEvent = lastEvents[channelId]
        val currentTime = System.currentTimeMillis()

        if (lastEvent == null) {
            lastEvents[channelId] = event
            return true
        }

        // 1. Same theme? Don't re-publish unless intensity change is significant (> 0.2)
        if (lastEvent.theme == event.theme) {
            val intensityDiff = Math.abs((lastEvent.intensity ?: 0.0) - (event.intensity ?: 0.0))
            if (intensityDiff < 0.2) {
                return false
            }
        }

        // 2. Grace Period: Prevent rapid flipping between themes
        val timeSinceLast = currentTime - lastEvent.timestamp
        if (timeSinceLast < gracePeriodMs) {
            // Bypass grace period for Emergency
            if (event.theme == "emergency") {
                logger.info("Bypassing grace period for emergency event in channel: $channelId")
                lastEvents[channelId] = event
                return true
            }
            
            logger.info("Grace period active for channel $channelId. Ignoring ${event.theme} (last was ${lastEvent.theme} ${timeSinceLast}ms ago)")
            return false
        }

        lastEvents[channelId] = event
        return true
    }
}
