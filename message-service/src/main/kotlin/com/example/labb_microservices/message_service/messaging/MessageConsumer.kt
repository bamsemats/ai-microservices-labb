package com.example.labb_microservices.message_service.messaging

import com.example.labb_microservices.message_service.handler.MessageWebSocketHandler
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
    private val webSocketHandler: MessageWebSocketHandler,
    private val objectMapper: ObjectMapper,
    private val messageProducer: MessageProducer
) {

    private val logger = LoggerFactory.getLogger(MessageConsumer::class.java)

    @RabbitListener(queues = [RabbitMQConfig.STORAGE_QUEUE_NAME])
    fun storeMessage(message: Message) {
        logger.info("Storing message: ${message.id}")
        messageRepository.save(message)
            .flatMap { savedMessage ->
                logger.info("Saved message to MongoDB: ${savedMessage.id}")
                messageProducer.deliverMessage(savedMessage)
                Mono.just(savedMessage)
            }
            .block()
    }

    @RabbitListener(queues = ["#{websocketQueue.name}"])
    fun deliverMessage(message: Message) {
        logger.info("Delivering message: ${message.id} to WebSockets in channel ${message.channelId}")
        try {
            val jsonMessage = objectMapper.writeValueAsString(message)
            
            if (message.receiverId == "all") {
                webSocketHandler.broadcastToChannel(message.channelId, jsonMessage)
            } else {
                val recipients = setOfNotNull(
                    message.receiverId.takeIf { it.isNotBlank() },
                    message.senderId.takeIf { it.isNotBlank() }
                )
                recipients.forEach { userId ->
                    webSocketHandler.sendMessageToUser(userId, message.channelId, jsonMessage)
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to serialize message for WebSocket", e)
            throw e
        }
    }

    @RabbitListener(queues = [RabbitMQConfig.AI_RESPONSE_QUEUE_NAME])
    fun consumeAiResponse(message: Message) {
        val messageId = message.id ?: return
        logger.debug("Received AI chunk for: $messageId")
        
        // Immediately deliver to WebSockets for low perceived latency
        messageProducer.deliverMessage(message)

        // Atomic update in MongoDB using $concat
        val query = Query(Criteria.where("id").`is`(messageId))
        val update = Update().set("id", messageId)
            .setOnInsert("senderId", message.senderId)
            .setOnInsert("receiverId", message.receiverId)
            .setOnInsert("channelId", message.channelId)
            .setOnInsert("authorType", message.authorType)
            .setOnInsert("timestamp", message.timestamp)
            .push("contentChunks", message.content) // Optionally store chunks
            // For simple content accumulation:
            // .set("content", ...) is tricky with $concat in Update.
            // Better to use a dedicated field or aggregation if we want true atomic append.
            // But ReactiveMongoTemplate.upsert with Update.push is atomic.
            // Let's stick to the prompt's suggestion of atomic upsert.
        
        mongoTemplate.upsert(query, update, Message::class.java)
            .doOnError { e -> logger.error("Failed to save AI chunk for $messageId", e) }
            .subscribe()
    }

    @RabbitListener(queues = ["#{adaptationQueue.name}"])
    fun consumeGenericEvent(eventData: Map<String, Any>) {
        val type = eventData["type"] as? String ?: "UI_ADAPTATION"
        logger.info("Received real-time event: $type")
        try {
            val jsonEvent = objectMapper.writeValueAsString(eventData)
            webSocketHandler.broadcastMessage(jsonEvent)
        } catch (e: Exception) {
            logger.error("Failed to serialize generic event for WebSocket", e)
        }
    }

    @RabbitListener(queues = ["#{contentInjectionQueue.name}"])
    fun consumeContentInjectionEvent(event: ContentInjectionEvent) {
        logger.info("Received Content Injection Event: ${event.contentType}")
        try {
            val jsonEvent = objectMapper.writeValueAsString(event)
            webSocketHandler.broadcastMessage(jsonEvent)
        } catch (e: Exception) {
            logger.error("Failed to serialize content injection event for WebSocket", e)
            throw e
        }
    }

    @RabbitListener(queues = ["#{presenceQueue.name}"])
    fun consumePresenceUpdate(event: PresenceUpdateEvent) {
        logger.info("Received Presence Update: ${event.userId} is ${event.status}")
        try {
            val jsonEvent = objectMapper.writeValueAsString(event)
            // Broadcast presence to all users as it affects the sidebar
            webSocketHandler.broadcastMessage(jsonEvent)
        } catch (e: Exception) {
            logger.error("Failed to serialize presence update for WebSocket", e)
            throw e
        }
    }
}
