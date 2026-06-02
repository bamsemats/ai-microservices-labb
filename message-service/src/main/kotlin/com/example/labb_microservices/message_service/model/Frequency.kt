package com.example.labb_microservices.message_service.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document(collection = "frequencies")
data class Frequency(
    @Id
    val id: String? = null,
    val name: String,
    val description: String? = null,
    val ownerId: String,
    val members: Set<String> = emptySet(),
    val createdAt: Instant = Instant.now()
)
