package com.example.labb_microservices.message_service.controller

import com.example.labb_microservices.message_service.client.UserGrpcClient
import com.example.labb_microservices.message_service.messaging.MessageProducer
import com.example.labb_microservices.message_service.model.Message
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono
import java.util.*

data class MessageRequest(val receiverId: String, val content: String)

@RestController
@RequestMapping("/messages")
class MessageController(
    private val userGrpcClient: UserGrpcClient,
    private val messageProducer: MessageProducer
) {

    @PostMapping
    fun sendMessage(@RequestBody request: MessageRequest): Mono<String> {
        return ReactiveSecurityContextHolder.getContext()
            .map { it.authentication.name }
            .defaultIfEmpty("anonymous")
            .flatMap { senderId ->
                Mono.fromCallable {
                    val message = Message(
                        id = UUID.randomUUID().toString(),
                        senderId = senderId,
                        receiverId = request.receiverId,
                        content = request.content,
                        authorType = AuthorType.USER
                    )
                    messageProducer.sendMessage(message)

                    if (request.content.contains("@ai", ignoreCase = true)) {
                        messageProducer.sendAiRequest(message)
                    }

                    "Message sent to queue by \${senderId}"
                }
            }
    }

    @GetMapping("/user/{userId}")
    fun getUserInfo(@PathVariable userId: String): Mono<String> {
        return Mono.fromCallable {
            val user = userGrpcClient.getUser(userId)
            "User: \${user.username}, Email: \${user.email}"
        }
    }

    @GetMapping
    fun getMessages(): Mono<String> {
        return Mono.just("messages")
    }
}
