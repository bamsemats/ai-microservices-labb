package com.example.labb_microservices.user_service.model

enum class PresenceStatus {
    ONLINE, AWAY, DND, OFFLINE
}

data class PresenceUpdateEvent(
    val userId: String,
    val username: String,
    val status: PresenceStatus,
    val type: String = "PRESENCE_UPDATE",
    val timestamp: Long = System.currentTimeMillis()
)
