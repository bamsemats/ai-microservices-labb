package com.example.labb_microservices.user_service.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "users")
data class User(
    @Id
    val id: String? = null,
    val username: String,
    val password: String,
    val email: String? = null, // Stores encrypted email
    @Indexed(unique = true, sparse = true)
    val emailHash: String? = null, // Stores hashed email for searching
    val enabled: Boolean = true,
    val displayName: String? = null,
    val bio: String? = null,
    val socialLinks: Map<String, String>? = null
)
