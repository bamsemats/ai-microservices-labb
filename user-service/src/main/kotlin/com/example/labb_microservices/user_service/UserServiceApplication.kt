package com.example.labb_microservices.user_service

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import com.example.labb_microservices.user_service.service.UserService
import org.slf4j.LoggerFactory
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@SpringBootApplication
class UserServiceApplication {
    private val logger = LoggerFactory.getLogger(UserServiceApplication::class.java)

    @Bean
    fun seedRunner(userService: UserService, env: org.springframework.core.env.Environment) = CommandLineRunner {
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
            val adminUsername = env.getProperty("admin.setup.username", "admin")
            val adminPassword = env.getProperty("admin.setup.password") 
                ?: throw RuntimeException("ADMIN_SETUP_PASSWORD is required for initial synchronization")
            val adminEmail = env.getProperty("admin.setup.email", "admin@adaptachat.io")

            val adminUser = com.example.labb_microservices.user_service.model.User(
                id = "system-admin",
                username = adminUsername,
                password = adminPassword, // TODO: Require forced password change on first login
                email = adminEmail,
                roles = listOf("ROLE_ADMIN", "ROLE_USER"),
                displayName = "System Administrator"
            )
            userService.register(adminUser)
                .doOnSuccess { logger.info("System admin account created/synchronized.") }
                .onErrorResume { e -> 
                    if (e is org.springframework.dao.DuplicateKeyException || e.message?.contains("exists", ignoreCase = true) == true) {
                        logger.info("System admin account already exists. Synchronization complete.")
                        Mono.empty()
                    } else {
                        logger.error("Failed to register system admin", e)
                        Mono.error(e)
                    }
                }
                .block(java.time.Duration.ofSeconds(10))

            logger.info("Seeding dummy test accounts...")
            val testUsers = listOf(
                com.example.labb_microservices.user_service.model.User(
                    id = "test-user-1",
                    username = "user1",
                    password = "password123",
                    email = "user1@example.com",
                    roles = listOf("ROLE_USER"),
                    displayName = "Beta Tester One"
                ),
                com.example.labb_microservices.user_service.model.User(
                    id = "test-user-2",
                    username = "user2",
                    password = "password123",
                    email = "user2@example.com",
                    roles = listOf("ROLE_USER"),
                    displayName = "Beta Tester Two"
                )
            )

            Flux.fromIterable(testUsers)
                .flatMap { u ->
                    userService.register(u)
                        .onErrorResume { Mono.empty() }
                }
                .collectList()
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
