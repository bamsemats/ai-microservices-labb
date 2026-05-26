package com.example.labb_microservices.message_service.repository

import com.example.labb_microservices.message_service.model.Frequency
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux

@Repository
interface FrequencyRepository : ReactiveMongoRepository<Frequency, String> {
    fun findByMembersContaining(userId: String): Flux<Frequency>
}
