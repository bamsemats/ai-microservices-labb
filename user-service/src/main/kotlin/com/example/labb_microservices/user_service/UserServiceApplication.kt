package com.example.labb_microservices.user_service

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import com.example.labb_microservices.user_service.service.UserService
import org.slf4j.LoggerFactory
import reactor.core.publisher.Mono

@SpringBootApplication
class UserServiceApplication {
    private val logger = LoggerFactory.getLogger(UserServiceApplication::class.java)

    @Bean
    fun seedBotsRunner(userService: UserService) = CommandLineRunner {
        val bots = listOf(
            "NexusPrime" to "Architect",
            "AdaptaAI" to "Assistant",
            "EchoFlow" to "Curator",
            "VibeCheck" to "Moderator",
            "HelpDesk" to "Support"
        )
        
        try {
            logger.info("Synchronizing bot accounts...")
            userService.seedBots(bots).block()
            logger.info("Bot accounts synchronization completed.")
        } catch (e: Exception) {
            logger.error("Bot accounts synchronization failed", e)
            throw e
        }
    }
}

fun main(args: Array<String>) {
    runApplication<UserServiceApplication>(*args)
}
