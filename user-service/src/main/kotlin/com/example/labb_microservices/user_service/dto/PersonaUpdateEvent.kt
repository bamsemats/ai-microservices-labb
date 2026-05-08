package com.example.labb_microservices.user_service.dto

data class PersonaUpdateEvent(
    val userId: String,
    val category: String,
    val value: String
)
