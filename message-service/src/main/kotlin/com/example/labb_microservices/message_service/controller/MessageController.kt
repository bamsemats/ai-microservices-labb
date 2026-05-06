package com.example.labb_microservices.message_service.controller

import com.example.labb_microservices.message_service.client.UserGrpcClient
import com.example.labb_microservices.message_service.messaging.MessageProducer
import com.example.labb_microservices.message_service.model.AuthorType
import com.example.labb_microservices.message_service.model.Message
import com.example.labb_microservices.message_service.service.PresenceService
import org.slf4j.LoggerFactory
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.util.*
import org.springframework.security.access.prepost.PreAuthorize

data class MessageRequest(val receiverId: String, val content: String, val channelId: String? = null)
data class BroadcastRequest(val content: String, val channelId: String? = null)

@RestController
@RequestMapping("/messages")
class MessageController(
    private val userGrpcClient: UserGrpcClient,
    private val messageProducer: MessageProducer,
    private val presenceService: PresenceService,
    @org.springframework.beans.factory.annotation.Value("\${app.test-mode.allowed:false}") private val isTestModeHeaderAllowed: Boolean
) {

    private val logger = LoggerFactory.getLogger(MessageController::class.java)

    @PostMapping
    fun sendMessage(
        @RequestBody request: MessageRequest,
        @RequestHeader("X-Adapta-Test-Mode", required = false) testMode: String?
    ): Mono<String> {
        return ReactiveSecurityContextHolder.getContext()
            .map { it.authentication.name }
            .flatMap { senderId ->
                Mono.fromCallable {
                    val idPrefix = if (isTestModeHeaderAllowed && testMode?.equals("true", ignoreCase = true) == true) "test-" else ""
                    val message = Message(
                        id = idPrefix + UUID.randomUUID().toString(),
                        senderId = senderId,
                        receiverId = request.receiverId,
                        channelId = request.channelId ?: "general",
                        content = request.content,
                        authorType = AuthorType.USER
                    )
                    processMessage(message)
                    "Message sent to queue by $senderId in channel ${message.channelId}"
                }
                .subscribeOn(Schedulers.boundedElastic())
            }
    }

    @PostMapping("/broadcast")
    @PreAuthorize("hasRole('ADMIN')")
    fun broadcastMessage(@RequestBody request: BroadcastRequest): Mono<String> {
        return ReactiveSecurityContextHolder.getContext()
            .flatMap { context ->
                val auth = context.authentication
                val senderId = auth.name
                Mono.fromCallable {
                    val message = Message(
                        id = UUID.randomUUID().toString(),
                        senderId = senderId,
                        receiverId = "all",
                        channelId = request.channelId ?: "all",
                        content = request.content,
                        authorType = AuthorType.USER
                    )
                    processMessage(message)
                    "Broadcast message sent by $senderId in channel ${message.channelId}"
                }
                .subscribeOn(Schedulers.boundedElastic())
            }
    }

    companion object {
        private val AI_MENTION_REGEX = Regex("(?i)(?:^|\\W)@ai(?:\\W|$)")
    }

    private fun processMessage(message: Message) {
        messageProducer.sendMessage(message)

        if (AI_MENTION_REGEX.containsMatchIn(message.content)) {
            try {
                messageProducer.sendAiRequest(message)
            } catch (e: Exception) {
                logger.error("Failed to trigger AI request for message ${message.id}", e)
            }
        }
    }

    @GetMapping("/user/{userId}")
    fun getUserInfo(@PathVariable userId: String): Mono<String> {
        return ReactiveSecurityContextHolder.getContext()
            .flatMap { context ->
                val auth = context.authentication
                val isAdmin = auth.authorities.any { it.authority == "ROLE_ADMIN" }
                val isSelf = auth.name == userId
                
                userGrpcClient.getUser(userId)
                    .map { user ->
                        if (isAdmin || isSelf) {
                            "User: ${user.username}, Email: ${user.email}"
                        } else {
                            "User: ${user.username}"
                        }
                    }
            }
    }

    @GetMapping
    fun getMessages(): Mono<String> {
        return Mono.just("messages")
    }

    @GetMapping("/presence")
    @PreAuthorize("hasRole('ADMIN')") // Restrict global presence to admins for now
    fun getOnlineUsers(): Flux<String> {
        return presenceService.getAllOnlineUsers()
    }
}
