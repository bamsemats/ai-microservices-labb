package com.example.labb_microservices.message_service.service

import com.example.labb_microservices.message_service.messaging.MessageProducer
import com.example.labb_microservices.message_service.model.Message
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.time.Instant

@Service
class MessageService(
    private val messageProducer: MessageProducer,
    private val userGrpcClient: com.example.labb_microservices.message_service.client.UserGrpcClient,
    private val messageRepository: com.example.labb_microservices.message_service.repository.MessageRepository,
    private val frequencyService: FrequencyService,
    private val presenceService: PresenceService,
    private val encryptionUtils: com.example.labb_microservices.common.security.EncryptionUtils,
    private val mongoTemplate: org.springframework.data.mongodb.core.ReactiveMongoTemplate
) {
    private val logger = LoggerFactory.getLogger(MessageService::class.java)

    private val AI_BOT_IDS = setOf("ai", "ai-bot", "adaptaai", "nexusprime", "echoflow", "vibecheck", "helpdesk")
    private val AI_MENTION_REGEX = Regex("(?i)(?:^|\\W)@(ai-bot|ai|adaptaai|nexusprime|echoflow|vibecheck|helpdesk)(?:\\W|$)")

    fun getOnlineUsers(): Flux<String> = presenceService.getAllOnlineUsers()

    fun processMessage(message: Message): Mono<Void> {
        return getUserWithFallback(message.senderId)
            .flatMap { userResponse ->
                val enrichedName = if (userResponse.displayName.isNotBlank()) userResponse.displayName else userResponse.username
                
                val sanitizedChannelId = if (message.channelId == "home" || message.channelId == "all") "general" else message.channelId
                val sanitizedReceiverId = if (message.receiverId == "home") "all" else message.receiverId
                
                val normalizedMessage = message.copy(
                    senderName = enrichedName,
                    channelId = sanitizedChannelId,
                    receiverId = sanitizedReceiverId
                )

                Mono.fromCallable {
                    logger.info("[TRACE] processMessage START - id: {}, sender: {}, senderName: {}, receiver: {}, channel: {}", 
                        normalizedMessage.id, normalizedMessage.senderId, normalizedMessage.senderName, normalizedMessage.receiverId, normalizedMessage.channelId)
                    
                    // 1. Send to storage queue
                    messageProducer.sendMessage(normalizedMessage)
                    logger.info("[TRACE] Sent message {} to storage queue", normalizedMessage.id)
                    
                    // 2. Check for AI triggers
                    val isAiRecipient = AI_BOT_IDS.contains(normalizedMessage.receiverId.lowercase())
                    val hasAiMention = AI_MENTION_REGEX.containsMatchIn(normalizedMessage.content)
                    
                    if (isAiRecipient || hasAiMention) {
                        logger.info("[TRACE] AI Trigger DETECTED for message {}. isAiRecipient={}, hasAiMention={}", 
                            normalizedMessage.id, isAiRecipient, hasAiMention)
                        try {
                            messageProducer.sendAiRequest(normalizedMessage)
                            logger.info("[TRACE] AI request sent to RabbitMQ for message {}", normalizedMessage.id)
                        } catch (e: Exception) {
                            logger.error("[TRACE] FAILED to send AI request to RabbitMQ", e)
                        }
                    }
                }.subscribeOn(Schedulers.boundedElastic())
            }.then()
    }

    fun searchMessages(
        q: String, 
        channelId: String?, 
        senderId: String?, 
        startDate: Instant? = null,
        endDate: Instant? = null,
        sentimentTheme: String? = null,
        minIntensity: Double? = null,
        principal: String, 
        isAdmin: Boolean
    ): Flux<Message> {
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
            val query = org.springframework.data.mongodb.core.query.Query(
                org.springframework.data.mongodb.core.query.Criteria.where("searchIndices").all(hashes)
            )

            if (!isAdmin) {
                val visibilityCriteria = org.springframework.data.mongodb.core.query.Criteria().orOperator(
                    org.springframework.data.mongodb.core.query.Criteria.where("senderId").`is`(principal),
                    org.springframework.data.mongodb.core.query.Criteria.where("receiverId").`is`(principal),
                    org.springframework.data.mongodb.core.query.Criteria.where("receiverId").`is`("all")
                )
                query.addCriteria(visibilityCriteria)
            }

            if (!channelId.isNullOrBlank()) {
                query.addCriteria(org.springframework.data.mongodb.core.query.Criteria.where("channelId").`is`(channelId))
            }

            if (!senderId.isNullOrBlank()) {
                query.addCriteria(org.springframework.data.mongodb.core.query.Criteria.where("senderId").`is`(senderId))
            }

            if (startDate != null || endDate != null) {
                val dateCriteria = org.springframework.data.mongodb.core.query.Criteria.where("timestamp")
                startDate?.let { dateCriteria.gte(it) }
                endDate?.let { dateCriteria.lte(it) }
                query.addCriteria(dateCriteria)
            }

            if (!sentimentTheme.isNullOrBlank()) {
                query.addCriteria(org.springframework.data.mongodb.core.query.Criteria.where("sentimentTheme").`is`(sentimentTheme))
            }

            if (minIntensity != null) {
                query.addCriteria(org.springframework.data.mongodb.core.query.Criteria.where("sentimentIntensity").gte(minIntensity))
            }

            query.limit(100)
            query.with(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "timestamp"))

            mongoTemplate.find(query, Message::class.java)
                .flatMap { encryptedMessage ->
                    Mono.fromCallable { decryptMessage(encryptedMessage) }
                        .subscribeOn(Schedulers.boundedElastic())
                }
        }
    }

    fun getMessages(receiverId: String?, channelId: String?, principal: String, isAdmin: Boolean): Flux<Message> {
        val authCheck = if (channelId != null && channelId != "general" && channelId != "global" && !isAdmin) {
            frequencyService.findById(channelId)
                .flatMap { freq ->
                    if (freq.members.contains(principal)) Mono.just(true)
                    else Mono.just(false)
                }
                .switchIfEmpty(Mono.defer {
                    val parts = channelId.split("-")
                    val isDm = parts.size == 2 && principal in parts
                    Mono.just(isDm)
                })
        } else {
            Mono.just(true)
        }

        return authCheck.flatMapMany { authorized ->
            if (!authorized) return@flatMapMany Flux.error<Message>(org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "Not a member of this frequency"))

            val query = when {
                channelId != null -> {
                    if (isAdmin) {
                        messageRepository.findAllByChannelId(channelId)
                    } else {
                        mongoTemplate.find(
                            org.springframework.data.mongodb.core.query.Query(
                                org.springframework.data.mongodb.core.query.Criteria.where("channelId").`is`(channelId)
                                    .andOperator(
                                        org.springframework.data.mongodb.core.query.Criteria().orOperator(
                                            org.springframework.data.mongodb.core.query.Criteria.where("senderId").`is`(principal),
                                            org.springframework.data.mongodb.core.query.Criteria.where("receiverId").`is`(principal),
                                            org.springframework.data.mongodb.core.query.Criteria.where("receiverId").`is`("all")
                                        )
                                    )
                            ),
                            Message::class.java
                        )
                    }
                }
                receiverId != null -> {
                    if (receiverId == principal) {
                        messageRepository.findAllByReceiverIdOrSenderId(principal, principal)
                    } else {
                        messageRepository.findAllBySenderIdAndReceiverId(principal, receiverId)
                            .mergeWith(messageRepository.findAllBySenderIdAndReceiverId(receiverId, principal))
                    }
                }
                isAdmin -> {
                    messageRepository.findAll()
                }
                else -> {
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

    private fun getUserWithFallback(userId: String): Mono<com.example.labb_microservices.proto.UserResponse> {
        if (AI_BOT_IDS.contains(userId.lowercase())) {
             return Mono.just(com.example.labb_microservices.proto.UserResponse.newBuilder().setUsername(userId).build())
        }
        return userGrpcClient.getUser(userId)
            .onErrorResume { e ->
                logger.debug("User $userId not found or error via gRPC: ${e.message}")
                Mono.just(com.example.labb_microservices.proto.UserResponse.newBuilder().setUsername(userId).build())
            }
            .defaultIfEmpty(com.example.labb_microservices.proto.UserResponse.newBuilder().setUsername(userId).build())
    }
}
