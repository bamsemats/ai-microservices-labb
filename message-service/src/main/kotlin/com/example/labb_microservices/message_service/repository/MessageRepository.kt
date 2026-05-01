package com.example.labb_microservices.message_service.repository

import com.example.labb_microservices.message_service.model.Message
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Repository

@Repository
interface MessageRepository : ReactiveMongoRepository<Message, String>
