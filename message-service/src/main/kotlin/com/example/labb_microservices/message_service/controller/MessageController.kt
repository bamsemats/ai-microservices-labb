package com.example.labb_microservices.message_service.controller

import com.example.labb_microservices.message_service.client.UserGrpcClient
import com.example.labb_microservices.message_service.model.AuthorType
import com.example.labb_microservices.message_service.model.Message
import com.example.labb_microservices.message_service.service.MessageService
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.*
import java.time.Instant
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.server.ResponseStatusException
import org.springframework.http.HttpStatus

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class MessageRequest(
    @field:NotBlank val receiverId: String,
    @field:NotBlank @field:Size(max = 5000) val content: String,
    val channelId: String? = null
)

data class BroadcastRequest(
    @field:NotBlank @field:Size(max = 5000) val content: String,
    val channelId: String? = null
)

@RestController
@RequestMapping("/messages")
class MessageController(
    private val userGrpcClient: UserGrpcClient,
    private val messageService: MessageService,
    @param:org.springframework.beans.factory.annotation.Value("\${app.test-mode.allowed:false}") private val isTestModeHeaderAllowed: Boolean
) {

    @PostMapping
    fun sendMessage(
        @Valid @RequestBody request: MessageRequest,
        @RequestHeader("X-Adapta-Test-Mode", required = false) testMode: String?
    ): Mono<String> {
        return ReactiveSecurityContextHolder.getContext()
            .flatMap { context ->
                val auth = context.authentication
                val senderId = auth.name
                val isAdmin = auth.authorities.any { it.authority == "ROLE_ADMIN" }
                
                if (request.receiverId == "all" && !isAdmin) {
                    return@flatMap Mono.error<String>(AccessDeniedException("Only admins can send broadcast messages"))
                }

                val idPrefix = if (isTestModeHeaderAllowed && testMode?.equals("true", ignoreCase = true) == true) "test-" else ""
                val metadata = mutableMapOf<String, String>()
                if (isTestModeHeaderAllowed && testMode?.equals("true", ignoreCase = true) == true) {
                    metadata["X-Adapta-Test-Mode"] = "true"
                }
                
                val message = Message(
                    id = idPrefix + UUID.randomUUID().toString(),
                    senderId = senderId,
                    receiverId = request.receiverId,
                    channelId = request.channelId ?: "general",
                    content = request.content,
                    authorType = AuthorType.USER,
                    metadata = metadata
                )
                
                messageService.processMessage(message)
                    .thenReturn("Message received: ${message.id}")
            }
    }

    @PostMapping("/broadcast")
    @PreAuthorize("hasRole('ADMIN')")
    fun broadcastMessage(@Valid @RequestBody request: BroadcastRequest): Mono<String> {
        return ReactiveSecurityContextHolder.getContext()
            .flatMap { context ->
                val auth = context.authentication
                val senderId = auth.name
                
                val message = Message(
                    id = UUID.randomUUID().toString(),
                    senderId = senderId,
                    receiverId = "all",
                    channelId = request.channelId ?: "global",
                    content = request.content,
                    authorType = AuthorType.USER
                )
                
                messageService.processMessage(message)
                    .thenReturn("Broadcast message sent by $senderId in channel ${message.channelId}")
            }
    }

    @GetMapping("/search")
    fun searchMessages(
        @RequestParam q: String,
        @RequestParam(required = false) channelId: String?,
        @RequestParam(required = false) senderId: String?,
        @RequestParam(required = false) startDate: String?,
        @RequestParam(required = false) endDate: String?,
        @RequestParam(required = false) sentimentTheme: String?,
        @RequestParam(required = false) minIntensity: Double?
    ): Flux<Message> {
        return ReactiveSecurityContextHolder.getContext()
            .flatMapMany { context ->
                val auth = context.authentication
                val principal = auth.name
                val isAdmin = auth.authorities.any { it.authority == "ROLE_ADMIN" }
                
                val start = startDate?.let { try { Instant.parse(it) } catch(e: Exception) { null } }
                val end = endDate?.let { try { Instant.parse(it) } catch(e: Exception) { null } }

                messageService.searchMessages(
                    q = q, 
                    channelId = channelId, 
                    senderId = senderId, 
                    startDate = start,
                    endDate = end,
                    sentimentTheme = sentimentTheme,
                    minIntensity = minIntensity,
                    principal = principal, 
                    isAdmin = isAdmin
                )
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
                    .onErrorResume { e ->
                        if (e is io.grpc.StatusRuntimeException && e.status.code == io.grpc.Status.Code.NOT_FOUND) {
                            Mono.error(ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"))
                        } else {
                            Mono.error(e)
                        }
                    }
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
    fun getMessages(
        @RequestParam(required = false) receiverId: String?,
        @RequestParam(required = false) channelId: String?
    ): Flux<Message> {
        return ReactiveSecurityContextHolder.getContext()
            .flatMapMany { context ->
                val auth = context.authentication
                val principal = auth.name
                val isAdmin = auth.authorities.any { it.authority == "ROLE_ADMIN" }

                messageService.getMessages(receiverId, channelId, principal, isAdmin)
            }
    }

    @GetMapping("/presence")
    @PreAuthorize("hasRole('ADMIN')")
    fun getOnlineUsers(): Flux<String> {
        return messageService.getOnlineUsers()
    }
}
