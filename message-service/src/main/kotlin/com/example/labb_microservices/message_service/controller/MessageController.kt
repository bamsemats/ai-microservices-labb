package com.example.labb_microservices.message_service.controller

import com.example.labb_microservices.message_service.client.UserGrpcClient
import com.example.labb_microservices.message_service.messaging.MessageProducer
import com.example.labb_microservices.message_service.model.AuthorType
import com.example.labb_microservices.message_service.model.Message
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.util.*

data class MessageRequest(val receiverId: String, val content: String, val channelId: String? = null)
data class BroadcastRequest(val content: String, val channelId: String? = null)

@RestController
@RequestMapping("/messages")
class MessageController(
    private val userGrpcClient: UserGrpcClient,
    private val messageProducer: MessageProducer
) {

    private val logger = org.slf4j.LoggerFactory.getLogger(MessageController::class.java)

    @PostMapping
    fun sendMessage(@RequestBody request: MessageRequest): Mono<String> {
        return ReactiveSecurityContextHolder.getContext()
            .map { it.authentication.name }
            .flatMap { senderId ->
                Mono.fromCallable {
                    val message = Message(
                        id = UUID.randomUUID().toString(),
                        senderId = senderId,
                        receiverId = request.receiverId,
                        channelId = request.channelId ?: "general",
                        content = request.content,
                        authorType = AuthorType.USER
                    )
                    processMessage(message, request.content)
                    "Message sent to queue by $senderId in channel ${message.channelId}"
                }
                .subscribeOn(Schedulers.boundedElastic())
            }
    }

    @PostMapping("/broadcast")
    fun broadcastMessage(@RequestBody request: BroadcastRequest): Mono<String> {
        return ReactiveSecurityContextHolder.getContext()
            .flatMap { context ->
                val auth = context.authentication
                val isAdmin = auth.authorities.any { it.authority == "ROLE_ADMIN" }
                if (!isAdmin) {
                    Mono.error<String>(org.springframework.security.access.AccessDeniedException("Only admins can broadcast"))
                } else {
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
                        processMessage(message, request.content)
                        "Broadcast message sent by $senderId in channel ${message.channelId}"
                    }
                    .subscribeOn(Schedulers.boundedElastic())
                }
            }
    }

    private fun processMessage(message: Message, content: String) {
        messageProducer.sendMessage(message)

        if (content.contains("@ai", ignoreCase = true)) {
            try {
                messageProducer.sendAiRequest(message)
            } catch (e: Exception) {
                logger.error("Failed to trigger AI request for message ${message.id}", e)
            }
        }
    }

    @GetMapping("/user/{userId}")
    fun getUserInfo(@PathVariable userId: String): Mono<String> {
        return userGrpcClient.getUser(userId)
            .map { user -> "User: ${user.username}, Email: ${user.email}" }
    }

    @GetMapping
    fun getMessages(): Mono<String> {
        return Mono.just("messages")
    }
}
