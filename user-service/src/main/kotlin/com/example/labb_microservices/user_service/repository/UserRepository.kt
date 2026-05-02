package com.example.labb_microservices.user_service.repository

import com.example.labb_microservices.user_service.model.User
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Mono

@Repository
interface UserRepository : ReactiveMongoRepository<User, String> {
    fun findByUsername(username: String): Mono<User>
    fun findByEmailHash(emailHash: String): Mono<User>
}
