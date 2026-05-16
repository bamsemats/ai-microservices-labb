package com.example.labb_microservices.feedback_service.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document(collection = "feedback")
data class Feedback(
    @Id
    val id: String? = null,
    val userId: String? = null,
    val rating: Int,
    val comment: String,
    val timestamp: Instant = Instant.now()
)
