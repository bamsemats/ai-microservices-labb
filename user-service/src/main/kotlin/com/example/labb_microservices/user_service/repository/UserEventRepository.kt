package com.example.labb_microservices.user_service.repository

import com.example.labb_microservices.user_service.model.UserEvent
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux

@Repository
interface UserEventRepository : ReactiveMongoRepository<UserEvent, String> {
    fun findByUserIdOrderByTimestampDesc(userId: String): Flux<UserEvent>
    fun findTop50ByOrderByTimestampDesc(): Flux<UserEvent>
}
