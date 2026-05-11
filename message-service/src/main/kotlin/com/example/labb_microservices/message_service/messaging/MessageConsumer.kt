package com.example.labb_microservices.message_service.messaging

import com.example.labb_microservices.common.security.EncryptionUtils
import com.example.labb_microservices.message_service.model.ContentInjectionEvent
import com.example.labb_microservices.message_service.model.Message
import com.example.labb_microservices.message_service.model.PresenceUpdateEvent
import com.example.labb_microservices.message_service.repository.MessageRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class MessageConsumer(
    private val messageRepository: MessageRepository,
    private val mongoTemplate: ReactiveMongoTemplate,
    private val deliveryService: com.example.labb_microservices.message_service.service.MessageDeliveryService,
    private val objectMapper: ObjectMapper,
    private val messageProducer: MessageProducer,
    private val encryptionUtils: EncryptionUtils
) {

    private val logger = LoggerFactory.getLogger(MessageConsumer::class.java)

    @RabbitListener(queues = [RabbitMQConfig.STORAGE_QUEUE_NAME])
    fun storeMessage(message: Message) {
        val messageToSave = if (message.id.isNullOrBlank()) {
            message.copy(
                id = java.util.UUID.randomUUID().toString(),
                content = encryptionUtils.encrypt(message.content),
                searchIndices = tokenizeAndHash(message.content)
            )
        } else {
            message.copy(
                content = encryptionUtils.encrypt(message.content),
                searchIndices = tokenizeAndHash(message.content)
            )
        }
        logger.info("Storing encrypted message: ${messageToSave.id}")
        messageRepository.save(messageToSave)
            .doOnSuccess {
                logger.info("Saved encrypted message to MongoDB: ${it.id}")
                messageProducer.deliverMessage(message) // Deliver original plain message
            }
            .block()
    }

    @RabbitListener(queues = ["#{websocketQueue.name}"])
    fun deliverMessage(message: Message) {
        logger.info("Delivering message: ${message.id} to WebSockets in channel ${message.channelId}")
        val jsonMessage = try {
            objectMapper.writeValueAsString(message)
        } catch (e: Exception) {
            logger.error("Failed to serialize message ${message.id} for WebSocket", e)
            return // Acknowledge and drop poison pill
        }

        try {
            if (message.receiverId == "all") {
                deliveryService.broadcastToChannel(message.channelId, jsonMessage)
            } else {
                val recipients = setOfNotNull(
                    message.receiverId.takeIf { it.isNotBlank() },
                    message.senderId.takeIf { it.isNotBlank() }
                )
                recipients.forEach { userId ->
                    deliveryService.sendMessageToUser(userId, jsonMessage)
                }
            }
        } catch (e: Exception) {
            logger.error("Transient failure broadcasting message ${message.id} to anonymous websocket queue", e)
            // Do not rethrow for anonymous queue to prevent infinite requeue
        }
    }

    @RabbitListener(queues = [RabbitMQConfig.AI_RESPONSE_QUEUE_NAME])
    fun consumeAiResponse(message: Message) {
        val messageId = message.id ?: return
        logger.debug("Received AI chunk for: $messageId")
        
        val encryptedContent = encryptionUtils.encrypt(message.content)
        val hashes = tokenizeAndHash(message.content)

        // Atomic update in MongoDB using Update.push to preserve duplicates and order
        val query = Query(Criteria.where("id").`is`(messageId))
        val update = Update().setOnInsert("id", messageId)
            .setOnInsert("senderId", message.senderId)
            .setOnInsert("receiverId", message.receiverId)
            .setOnInsert("channelId", message.channelId)
            .setOnInsert("authorType", message.authorType)
            .setOnInsert("timestamp", message.timestamp)
            .setOnInsert("content", encryptedContent) // Set initial encrypted content on insert
            .push("contentChunks", encryptedContent)
        
        if (hashes.isNotEmpty()) {
            update.addToSet("searchIndices").each(*hashes.toTypedArray())
        }
        
        // Block to ensure persistence before delivery for consistency
        mongoTemplate.upsert(query, update, Message::class.java).block()
        
        // Deliver to WebSockets after successful persistence
        messageProducer.deliverMessage(message)
    }

    private fun tokenizeAndHash(content: String): Set<String> {
        return content.lowercase()
            .split(Regex("[^\\p{L}\\p{N}]+"))
            .filter { it.isNotBlank() && it.length > 1 }
            .map { encryptionUtils.hash(it) }
            .toSet()
    }

    @RabbitListener(queues = ["#{adaptationQueue.name}"])
    fun consumeGenericEvent(eventData: Map<String, Any>) {
        val type = eventData["type"] as? String ?: "UI_ADAPTATION"
        val channelId = eventData["channelId"] as? String
        
        logger.info("Received real-time event: $type for channel: ${channelId ?: "global"}")
        val jsonEvent = try {
            objectMapper.writeValueAsString(eventData)
        } catch (e: Exception) {
            logger.error("Failed to serialize generic event for WebSocket", e)
            return
        }

        try {
            if (!channelId.isNullOrBlank()) {
                deliveryService.broadcastToChannel(channelId, jsonEvent)
            } else {
                deliveryService.broadcastMessage(jsonEvent)
            }
        } catch (e: Exception) {
            logger.error("Transient failure broadcasting generic event", e)
        }
    }

    @RabbitListener(queues = [RabbitMQConfig.EVENT_STORAGE_QUEUE_NAME])
    fun consumeEventStorage(eventData: Map<String, Any>) {
        val type = eventData["type"] as? String ?: return
        
        when (type) {
            "READ_RECEIPT" -> {
                val messageId = eventData["messageId"] as? String ?: return
                val userId = eventData["userId"] as? String ?: return
                val channelId = eventData["channelId"] as? String
                
                if (channelId.isNullOrBlank()) {
                    logger.warn("Dropping READ_RECEIPT event with missing/blank channelId. MessageId: $messageId, UserId: $userId")
                    return
                }
                
                logger.info("Processing read receipt for message $messageId by user $userId in channel $channelId")
                
                val query = Query(Criteria.where("id").`is`(messageId))
                val update = Update().addToSet("readBy", userId)
                
                mongoTemplate.updateFirst(query, update, Message::class.java)
                    .doOnSuccess { result ->
                        if (result.matchedCount > 0) {
                            try {
                                // Broadcast the update back to the channel
                                val jsonEvent = objectMapper.writeValueAsString(eventData)
                                deliveryService.broadcastToChannel(channelId!!, jsonEvent)
                            } catch (e: Exception) {
                                logger.error("Failed to broadcast read receipt for message $messageId", e)
                            }
                        } else {
                            logger.warn("Read receipt ignored: message $messageId not found")
                        }
                    }
                    .block()
            }
        }
    }

    @RabbitListener(queues = ["#{contentInjectionQueue.name}"])
    fun consumeContentInjectionEvent(event: ContentInjectionEvent) {
        logger.info("Received Content Injection Event: ${event.contentType}")
        val jsonEvent = try {
            objectMapper.writeValueAsString(event)
        } catch (e: Exception) {
            logger.error("Failed to serialize content injection event for WebSocket", e)
            return
        }

        try {
            deliveryService.broadcastMessage(jsonEvent)
        } catch (e: Exception) {
            logger.error("Transient failure broadcasting content injection event", e)
        }
    }

    @RabbitListener(queues = ["#{presenceQueue.name}"])
    fun consumePresenceUpdate(event: PresenceUpdateEvent) {
        logger.info("Received Presence Update: ${event.userId} is ${event.status}")
        val jsonEvent = try {
            objectMapper.writeValueAsString(event)
        } catch (e: Exception) {
            logger.error("Failed to serialize presence update for user ${event.userId}", e)
            return
        }

        try {
            // Broadcast presence to all users as it affects the sidebar
            deliveryService.broadcastMessage(jsonEvent)
        } catch (e: Exception) {
            logger.error("Transient failure broadcasting presence update for user ${event.userId}", e)
        }
    }
}
