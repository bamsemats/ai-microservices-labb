package com.example.labb_microservices.message_service.model

enum class PresenceStatus {
    ONLINE, AWAY, DND, OFFLINE
}

data class PresenceUpdateEvent(
    val type: String = "PRESENCE_UPDATE",
    val userId: String,
    val username: String,
    val status: PresenceStatus,
    val timestamp: Long = System.currentTimeMillis()
)
