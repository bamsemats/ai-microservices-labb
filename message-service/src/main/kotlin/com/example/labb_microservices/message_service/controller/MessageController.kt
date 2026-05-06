package com.example.labb_microservices.message_service.controller

import com.example.labb_microservices.common.security.EncryptionUtils
import com.example.labb_microservices.message_service.client.UserGrpcClient
import com.example.labb_microservices.message_service.messaging.MessageProducer
import com.example.labb_microservices.message_service.model.AuthorType
import com.example.labb_microservices.message_service.model.Message
import com.example.labb_microservices.message_service.repository.MessageRepository
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
    private val messageProducer: MessageProducer,
    private val presenceService: PresenceService,
    private val messageRepository: MessageRepository,
    private val encryptionUtils: EncryptionUtils,
    @org.springframework.beans.factory.annotation.Value("\${app.test-mode.allowed:false}") private val isTestModeHeaderAllowed: Boolean
) {

    private val logger = LoggerFactory.getLogger(MessageController::class.java)

    @PostMapping
    fun sendMessage(
        @Valid @RequestBody request: MessageRequest,
        @RequestHeader("X-Adapta-Test-Mode", required = false) testMode: String?
    ): Mono<String> {
        return ReactiveSecurityContextHolder.getContext()
            .map { it.authentication.name }
            .flatMap { senderId ->
                Mono.fromCallable {
                    val idPrefix = if (isTestModeHeaderAllowed && testMode?.equals("true", ignoreCase = true) == true) "test-" else ""
                    val channelId = request.channelId?.takeIf { it.isNotBlank() } ?: "general"
                    val message = Message(
                        id = idPrefix + UUID.randomUUID().toString(),
                        senderId = senderId,
                        receiverId = request.receiverId,
                        channelId = channelId,
                        content = request.content,
                        authorType = AuthorType.USER
                    )
                    processMessage(message)
                    "Message sent to queue by $senderId in channel $channelId"
                }
                .subscribeOn(Schedulers.boundedElastic())
            }
    }

    @PostMapping("/broadcast")
    @PreAuthorize("hasRole('ADMIN')")
    fun broadcastMessage(@Valid @RequestBody request: BroadcastRequest): Mono<String> {
        return ReactiveSecurityContextHolder.getContext()
            .flatMap { context ->
                val auth = context.authentication
                val senderId = auth.name
                Mono.fromCallable {
                    val channelId = request.channelId?.takeIf { it.isNotBlank() } ?: "all"
                    val message = Message(
                        id = UUID.randomUUID().toString(),
                        senderId = senderId,
                        receiverId = "all",
                        channelId = channelId,
                        content = request.content,
                        authorType = AuthorType.USER
                    )
                    processMessage(message)
                    "Broadcast message sent by $senderId in channel $channelId"
                }
                .subscribeOn(Schedulers.boundedElastic())
            }
    }

    @GetMapping("/search")
    fun searchMessages(@RequestParam q: String): Flux<Message> {
        if (q.isBlank() || q.length < 2) return Flux.empty()
        
        val searchHash = encryptionUtils.hash(q.lowercase().trim())
        logger.info("Searching for blind index: $searchHash")
        
        return messageRepository.findAllBySearchIndicesContaining(searchHash)
            .map { encryptedMessage ->
                try {
                    encryptedMessage.copy(
                        content = encryptionUtils.decrypt(encryptedMessage.content)
                    )
                } catch (e: Exception) {
                    logger.error("Failed to decrypt message ${encryptedMessage.id}", e)
                    encryptedMessage.copy(content = "[DECRYPTION_ERROR]")
                }
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
    fun getMessages(
        @RequestParam(required = false) receiverId: String?,
        @RequestParam(required = false) channelId: String?
    ): Flux<Message> {
        val query = if (channelId != null) {
            messageRepository.findAllByChannelId(channelId)
        } else if (receiverId != null) {
            // For DM, we want messages where (sender=me AND receiver=them) OR (sender=them AND receiver=me)
            // But for simplicity of the repo method, we'll just filter after fetching by receiverId OR senderId
            // Actually, findByReceiverIdOrSenderId(receiverId, receiverId) should work if receiverId is the peer
            messageRepository.findAllByReceiverIdOrSenderId(receiverId, receiverId)
        } else {
            messageRepository.findAll()
        }

        return query.map { encryptedMessage ->
            try {
                encryptedMessage.copy(
                    content = encryptionUtils.decrypt(encryptedMessage.content)
                )
            } catch (e: Exception) {
                logger.error("Failed to decrypt message ${encryptedMessage.id}", e)
                encryptedMessage.copy(content = "[DECRYPTION_ERROR]")
            }
        }
    }

    @GetMapping("/presence")
    @PreAuthorize("hasRole('ADMIN')") // Restrict global presence to admins for now
    fun getOnlineUsers(): Flux<String> {
        return presenceService.getAllOnlineUsers()
    }
}
