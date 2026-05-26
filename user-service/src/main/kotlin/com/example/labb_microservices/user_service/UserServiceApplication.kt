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
    fun seedRunner(userService: UserService) = CommandLineRunner {
        val bots = listOf(
            "NexusPrime" to "Architect",
            "AdaptaAI" to "Assistant",
            "EchoFlow" to "Curator",
            "VibeCheck" to "Moderator",
            "HelpDesk" to "Support"
        )
        
        try {
            logger.info("Synchronizing bot accounts...")
            userService.seedBots(bots).block(java.time.Duration.ofSeconds(30))
            
            logger.info("Ensuring system admin account exists...")
            val adminUser = com.example.labb_microservices.user_service.model.User(
                id = "system-admin",
                username = "admin",
                password = "admin-password-2026", // In production this would be externalized or changed on first login
                email = "admin@adaptachat.io",
                roles = listOf("ROLE_ADMIN", "ROLE_USER"),
                displayName = "System Administrator"
            )
            userService.register(adminUser)
                .onErrorResume { Mono.empty() } // Ignore if already exists
                .block(java.time.Duration.ofSeconds(10))
            
            logger.info("System stabilization completed.")
        } catch (e: Exception) {
            logger.error("System synchronization failed", e)
            throw e
        }
    }
}

fun main(args: Array<String>) {
    runApplication<UserServiceApplication>(*args)
}
