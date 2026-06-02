package com.example.labb_microservices.message_service.model

import java.io.Serializable
import java.time.LocalDateTime

data class ReadReceiptEvent(
    val type: String = "READ_RECEIPT",
    val messageId: String,
    val userId: String,
    val channelId: String,
    val timestamp: LocalDateTime = LocalDateTime.now()
) : Serializable
