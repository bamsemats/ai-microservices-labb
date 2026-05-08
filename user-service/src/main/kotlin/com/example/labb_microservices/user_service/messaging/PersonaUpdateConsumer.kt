package com.example.labb_microservices.user_service.messaging

import com.example.labb_microservices.user_service.config.RabbitConfig
import com.example.labb_microservices.user_service.dto.PersonaUpdateEvent
import com.example.labb_microservices.user_service.service.UserService
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Component

@Component
class PersonaUpdateConsumer(
    private val userService: UserService
) {
    private val logger = LoggerFactory.getLogger(PersonaUpdateConsumer::class.java)

    @RabbitListener(queues = [RabbitConfig.PERSONA_UPDATE_QUEUE])
    fun handlePersonaUpdate(event: PersonaUpdateEvent) {
        logger.info("Received persona update for user {}: {} -> {}", event.userId, event.category, event.value)
        
        try {
            userService.updateBioWithFact(event.userId, event.category, event.value)
                .doOnSuccess { logger.info("Successfully updated bio for user {}", event.userId) }
                .doOnError { e -> logger.error("Failed to update bio for user {}:", event.userId, e) }
                .block()
        } catch (e: Exception) {
            logger.error("Error processing persona update for user {}:", event.userId, e)
            throw e // Rethrow to trigger AMQP retry if configured
        }
    }
}
