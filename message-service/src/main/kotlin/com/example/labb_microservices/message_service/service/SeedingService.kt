package com.example.labb_microservices.message_service.service

import com.example.labb_microservices.message_service.model.AuthorType
import com.example.labb_microservices.message_service.model.Message
import com.example.labb_microservices.message_service.messaging.MessageProducer
import com.example.labb_microservices.message_service.repository.MessageRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Instant
import java.util.*

@Service
@ConditionalOnProperty(name = ["app.seeding.enabled"], havingValue = "true", matchIfMissing = true)
class SeedingService(
    private val messageProducer: MessageProducer,
    private val messageRepository: MessageRepository,
    private val presenceService: PresenceService,
    private val encryptionUtils: com.example.labb_microservices.common.security.EncryptionUtils
) : CommandLineRunner {
    private val logger = LoggerFactory.getLogger(SeedingService::class.java)

    // Using a list of "Living Bots" that we want to simulate
    private val bots = listOf(
        Bot("NexusPrime", "Architect"),
        Bot("AdaptaAI", "Assistant"),
        Bot("EchoFlow", "Curator"),
        Bot("VibeCheck", "Moderator"),
        Bot("HelpDesk", "Support")
    )

    data class Bot(val name: String, val role: String)

    fun seedData(): Mono<Void> {
        return Flux.fromIterable(bots)
            .flatMap { bot ->
                // Set bot online in presence service (Redis)
                presenceService.setBotOnline(bot.name)
                    .then(Mono.defer {
                        // Create a welcome message from the bot if it doesn't exist
                        val messageId = "seed-${bot.name}-welcome"
                        messageRepository.existsById(messageId)
                            .flatMap { exists ->
                                if (exists) {
                                    Mono.empty()
                                } else {
                                    val content = "Hello! I am ${bot.name}, the ${bot.role} of this frequency. How can I assist your synchronization today?"
                                    val welcomeMessage = Message(
                                        id = messageId,
                                        senderId = bot.name,
                                        senderName = bot.name,
                                        receiverId = "all",
                                        channelId = "general",
                                        content = encryptionUtils.encrypt(content),
                                        authorType = AuthorType.BOT,
                                        timestamp = Instant.now()
                                    )
                                    messageRepository.save(welcomeMessage)
                                        .doOnSuccess { 
                                            // Deliver plain text for WebSocket
                                            messageProducer.deliverMessage(it.copy(content = content)) 
                                        }
                                }
                            }
                            .then()
                    })
            }
            .then()
            .doOnSuccess { logger.info("Data seeding completed for living bots.") }
    }

    override fun run(vararg args: String?) {
        // Delay slightly to ensure infrastructure is ready
        Mono.delay(java.time.Duration.ofSeconds(5))
            .flatMap { seedData() }
            .subscribe(
                { logger.info("Seeding runner finished successfully") },
                { e -> logger.error("Seeding runner failed", e) }
            )
    }
}
