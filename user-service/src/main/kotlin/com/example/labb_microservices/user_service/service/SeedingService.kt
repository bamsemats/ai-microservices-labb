package com.example.labb_microservices.user_service.service

import com.example.labb_microservices.user_service.model.User
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.core.env.Environment
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Duration

@Service
class SeedingService(
    private val userService: UserService,
    private val env: Environment
) : CommandLineRunner {

    private val logger = LoggerFactory.getLogger(SeedingService::class.java)

    override fun run(vararg args: String?) {
        val bots = listOf(
            "NexusPrime" to "Architect",
            "AdaptaAI" to "Assistant",
            "EchoFlow" to "Curator",
            "VibeCheck" to "Moderator",
            "HelpDesk" to "Support"
        )
        
        try {
            logger.info("Synchronizing bot accounts...")
            userService.seedBots(bots).block(Duration.ofSeconds(30))
            
            logger.info("Ensuring system admin account exists...")
            val adminUsername = env.getProperty("admin.setup.username", "admin")
            val adminPassword = env.getProperty("admin.setup.password") 
                ?: throw RuntimeException("ADMIN_SETUP_PASSWORD is required for initial synchronization")
            val adminEmail = env.getProperty("admin.setup.email", "admin@adaptachat.io")

            val adminUser = User(
                id = "system-admin",
                username = adminUsername,
                password = adminPassword,
                email = adminEmail,
                roles = listOf("ROLE_ADMIN", "ROLE_USER"),
                displayName = "System Administrator",
                metadata = mapOf("forcePasswordChange" to "true")
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
                .block(Duration.ofSeconds(10))

            val isDev = env.activeProfiles.contains("dev")
            val shouldSeedUsers = env.getProperty("seed.test.users", Boolean::class.java, false) || isDev

            if (shouldSeedUsers) {
                logger.info("Seeding dummy test accounts...")
                val testUsers = listOf(
                    User(
                        id = "test-user-1",
                        username = env.getProperty("test.user1.username", "user1"),
                        password = env.getProperty("test.user1.password", "password123"),
                        email = env.getProperty("test.user1.email", "user1@example.com"),
                        roles = listOf("ROLE_USER"),
                        displayName = "Beta Tester One"
                    ),
                    User(
                        id = "test-user-2",
                        username = env.getProperty("test.user2.username", "user2"),
                        password = env.getProperty("test.user2.password", "password123"),
                        email = env.getProperty("test.user2.email", "user2@example.com"),
                        roles = listOf("ROLE_USER"),
                        displayName = "Beta Tester Two"
                    )
                )

                Flux.fromIterable(testUsers)
                    .flatMap { u ->
                        userService.register(u)
                            .onErrorResume { e ->
                                if (e is org.springframework.dao.DuplicateKeyException || e.message?.contains("exists", ignoreCase = true) == true) {
                                    Mono.empty()
                                } else {
                                    logger.warn("Non-critical failure seeding test user ${u.username}: ${e.message}")
                                    Mono.empty() // Still continue but log it
                                }
                            }
                    }
                    .collectList()
                    .block(Duration.ofSeconds(10))
            } else {
                logger.info("Skipping test account seeding (seed.test.users=false).")
            }
            
            logger.info("System stabilization completed.")
        } catch (e: Exception) {
            logger.error("System synchronization failed", e)
            // We don't want to crash the whole app if seeding fails, but for now we follow original logic
            throw e
        }
    }
}
