package com.example.labb_microservices.ai_service.logic

import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicInteger

@Component
class AiReadinessIndicator : HealthIndicator {

    private val activeRequests = AtomicInteger(0)
    private val maxConcurrentRequests = 10 // Threshold for readiness

    fun incrementActiveRequests() = activeRequests.incrementAndGet()
    fun decrementActiveRequests() = activeRequests.decrementAndGet()

    override fun health(): Health {
        val currentCount = activeRequests.get()
        return if (currentCount < maxConcurrentRequests) {
            Health.up()
                .withDetail("activeRequests", currentCount)
                .withDetail("maxThreshold", maxConcurrentRequests)
                .build()
        } else {
            Health.outOfService()
                .withDetail("activeRequests", currentCount)
                .withDetail("maxThreshold", maxConcurrentRequests)
                .withDetail("reason", "Overloaded: Max concurrent LLM requests reached")
                .build()
        }
    }
}
