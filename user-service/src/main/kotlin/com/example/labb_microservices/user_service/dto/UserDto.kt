package com.example.labb_microservices.user_service.dto

data class UserDto(
    val id: String?,
    val username: String,
    val email: String? = null,
    val enabled: Boolean = true,
    val displayName: String? = null,
    val bio: String? = null,
    val socialLinks: Map<String, String>? = null
)
