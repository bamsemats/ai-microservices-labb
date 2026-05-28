package com.example.labb_microservices.user_service.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

enum class EventType {
    LOGIN, LOGOUT, SEND_MESSAGE, ADD_FRIEND, CREATE_FREQUENCY, AI_INTERACTION, THEME_CHANGE
}

@Document(collection = "user_events")
data class UserEvent(
    @Id
    val id: String? = null,
    @Indexed
    val userId: String,
    @Indexed
    val eventType: EventType,
    val details: String? = null,
    @Indexed
    val timestamp: Instant = Instant.now()
)
