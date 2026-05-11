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
            .flatMap { context ->
                val auth = context.authentication
                val senderId = auth.name
                val isAdmin = auth.authorities.any { it.authority == "ROLE_ADMIN" }
                
                if (request.receiverId == "all" && !isAdmin) {
                    return@flatMap Mono.error<String>(AccessDeniedException("Only admins can send broadcast messages"))
                }

                userGrpcClient.getUser(senderId)
                    .onErrorResume { e ->
                        if (e is io.grpc.StatusRuntimeException && e.status.code == io.grpc.Status.Code.NOT_FOUND) {
                            logger.debug("User $senderId not found in user-service, using ID as name")
                        } else {
                            logger.error("Failed to lookup user $senderId via gRPC: ${e.message}")
                        }
                        Mono.just(com.example.labb_microservices.proto.UserResponse.newBuilder().setUsername(senderId).build())
                    }
                    .defaultIfEmpty(com.example.labb_microservices.proto.UserResponse.newBuilder().setUsername(senderId).build())
                    .flatMap { userResponse ->
                        Mono.fromCallable {
                            val idPrefix = if (isTestModeHeaderAllowed && testMode?.equals("true", ignoreCase = true) == true) "test-" else ""
                            val channelId = request.channelId?.takeIf { it.isNotBlank() } ?: "general"
                            val message = Message(
                                id = idPrefix + UUID.randomUUID().toString(),
                                senderId = senderId,
                                senderName = userResponse.username,
                                receiverId = request.receiverId,
                                channelId = channelId,
                                content = request.content,
                                authorType = AuthorType.USER
                            )
                            processMessage(message)
                            "Message sent to queue by $senderId in channel $channelId"
                        }
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
                
                userGrpcClient.getUser(senderId)
                    .onErrorResume { e ->
                        if (e is io.grpc.StatusRuntimeException && e.status.code == io.grpc.Status.Code.NOT_FOUND) {
                            logger.debug("User $senderId not found in user-service, using ID as name")
                        } else {
                            logger.error("Failed to lookup user $senderId via gRPC: ${e.message}")
                        }
                        Mono.just(com.example.labb_microservices.proto.UserResponse.newBuilder().setUsername(senderId).build())
                    }
                    .defaultIfEmpty(com.example.labb_microservices.proto.UserResponse.newBuilder().setUsername(senderId).build())
                    .flatMap { userResponse ->
                        Mono.fromCallable {
                            val channelId = request.channelId?.takeIf { it.isNotBlank() } ?: "all"
                            val message = Message(
                                id = UUID.randomUUID().toString(),
                                senderId = senderId,
                                senderName = userResponse.username,
                                receiverId = "all",
                                channelId = channelId,
                                content = request.content,
                                authorType = AuthorType.USER
                            )
                            processMessage(message)
                            "Broadcast message sent by $senderId in channel $channelId"
                        }
                    }
                .subscribeOn(Schedulers.boundedElastic())
            }
    }

    @GetMapping("/search")
    fun searchMessages(@RequestParam q: String): Flux<Message> {
        if (q.isBlank() || q.length < 2) return Flux.empty()
        
        val tokens = q.lowercase()
            .split(Regex("[^\\p{L}\\p{N}]+"))
            .filter { it.isNotBlank() && it.length > 1 }
        
        if (tokens.isEmpty()) return Flux.empty()
        
        return Mono.fromCallable { 
            tokens.map { encryptionUtils.hash(it) } 
        }
        .subscribeOn(Schedulers.boundedElastic())
        .flatMapMany { hashes ->
            logger.info("Searching for blind indices. Token count: ${tokens.size}")
            
            ReactiveSecurityContextHolder.getContext()
                .flatMapMany { context ->
                    val auth = context.authentication
                    val principal = auth.name
                    val isAdmin = auth.authorities.any { it.authority == "ROLE_ADMIN" }
                    
                    messageRepository.findAllBySearchIndicesContainingAll(hashes)
                        .flatMap { encryptedMessage ->
                            // Check if user is allowed to see this message
                            val isParticipant = encryptedMessage.senderId == principal || 
                                               encryptedMessage.receiverId == principal || 
                                               (encryptedMessage.receiverId == "all" && encryptedMessage.channelId == "global")
                            
                            if (isAdmin || isParticipant) {
                                Mono.fromCallable { decryptMessage(encryptedMessage) }
                                    .subscribeOn(Schedulers.boundedElastic())
                            } else {
                                Mono.empty()
                            }
                        }
                }
        }
    }

    companion object {
        private val AI_MENTION_REGEX = Regex("(?i)(?:^|\\W)@(ai|ai-bot|AdaptaAI|NexusPrime|EchoFlow|VibeCheck|HelpDesk)(?:\\W|$)")
    }

    private fun processMessage(message: Message) {
        messageProducer.sendMessage(message)
        
        try {
            messageProducer.sendSentimentRequest(message)
        } catch (e: Exception) {
            logger.error("Failed to trigger sentiment request for message ${message.id}", e)
        }

        val isAiRecipient = message.receiverId == "AdaptaAI" || message.receiverId == "ai-bot"
        if (isAiRecipient || AI_MENTION_REGEX.containsMatchIn(message.content)) {
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
                    .onErrorResume { e ->
                        if (e is io.grpc.StatusRuntimeException && e.status.code == io.grpc.Status.Code.NOT_FOUND) {
                            Mono.empty()
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

                val query = when {
                    channelId != null -> {
                        if (isAdmin) {
                            messageRepository.findAllByChannelId(channelId)
                        } else {
                            Flux.empty()
                        }
                    }
                    receiverId != null -> {
                        // Principal must be one of the participants
                        if (receiverId == principal) {
                            messageRepository.findAllByReceiverIdOrSenderId(principal, principal)
                        } else {
                            // Find DMs between principal and receiverId
                            messageRepository.findAllBySenderIdAndReceiverId(principal, receiverId)
                                .mergeWith(messageRepository.findAllBySenderIdAndReceiverId(receiverId, principal))
                        }
                    }
                    isAdmin -> {
                        messageRepository.findAll()
                    }
                    else -> {
                        // Non-admins can only see messages they are part of
                        messageRepository.findAllByReceiverIdOrSenderId(principal, principal)
                            .mergeWith(messageRepository.findAllByReceiverId("all"))
                    }
                }

                query.flatMap { encryptedMessage ->
                    Mono.fromCallable { decryptMessage(encryptedMessage) }
                        .subscribeOn(Schedulers.boundedElastic())
                }
            }
    }

    private fun decryptMessage(encryptedMessage: Message): Message {
        return try {
            encryptedMessage.copy(
                content = encryptionUtils.decrypt(encryptedMessage.content)
            )
        } catch (e: Exception) {
            logger.error("Failed to decrypt message ${encryptedMessage.id}", e)
            encryptedMessage.copy(content = "[DECRYPTION_ERROR]")
        }
    }

    @GetMapping("/presence")
    fun getOnlineUsers(): Flux<String> {
        return presenceService.getAllOnlineUsers()
    }
}
