package com.example.labb_microservices.feedback_service.model

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document(collection = "feedback")
data class Feedback(
    @Id
    val id: String? = null,
    val userId: String? = null,
    @field:Min(1) @field:Max(5)
    val rating: Int,
    val comment: String,
    val timestamp: Instant = Instant.now()
)
