package com.example.labb_microservices.feedback_service.repository

import com.example.labb_microservices.feedback_service.model.Feedback
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Repository

@Repository
interface FeedbackRepository : ReactiveMongoRepository<Feedback, String>
