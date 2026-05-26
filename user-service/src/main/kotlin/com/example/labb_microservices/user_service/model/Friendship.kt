package com.example.labb_microservices.user_service.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

enum class FriendshipStatus {
    PENDING, ACCEPTED
}

@Document(collection = "friendships")
@CompoundIndex(name = "user_friend_idx", def = "{'userId': 1, 'friendId': 1}", unique = true)
data class Friendship(
    @Id
    val id: String? = null,
    val userId: String,
    val friendId: String,
    val status: FriendshipStatus = FriendshipStatus.PENDING,
    val createdAt: Instant = Instant.now()
)
