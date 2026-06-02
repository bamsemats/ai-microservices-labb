package com.example.labb_microservices.user_service.repository

import com.example.labb_microservices.user_service.model.Friendship
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Repository
interface FriendshipRepository : ReactiveMongoRepository<Friendship, String> {
    fun findByUserId(userId: String): Flux<Friendship>
    fun findByFriendId(friendId: String): Flux<Friendship>
    fun findByUserIdAndFriendId(userId: String, friendId: String): Mono<Friendship>
    fun deleteAllByUserId(userId: String): Mono<Void>
    fun deleteAllByFriendId(friendId: String): Mono<Void>
}
