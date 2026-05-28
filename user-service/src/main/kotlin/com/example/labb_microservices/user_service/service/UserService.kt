package com.example.labb_microservices.user_service.service

import com.example.labb_microservices.common.security.EncryptionUtils
import com.example.labb_microservices.user_service.model.User
import com.example.labb_microservices.user_service.repository.UserRepository
import com.example.labb_microservices.user_service.model.PresenceStatus
import com.example.labb_microservices.user_service.repository.PresenceTracker
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.core.publisher.Flux
import org.slf4j.LoggerFactory
import java.util.*

import com.example.labb_microservices.user_service.model.Friendship
import com.example.labb_microservices.user_service.model.FriendshipStatus
import com.example.labb_microservices.user_service.repository.FriendshipRepository

@Service
class UserService(
    private val userRepository: UserRepository,
    private val friendshipRepository: FriendshipRepository,
    private val passwordEncoder: PasswordEncoder,
    private val encryptionUtils: EncryptionUtils,
    private val presenceTracker: PresenceTracker
) {
    private val logger = LoggerFactory.getLogger(UserService::class.java)

    fun searchUsers(query: String, page: Int, size: Int): Mono<org.springframework.data.domain.Page<User>> {
        val pageable = org.springframework.data.domain.PageRequest.of(page, size)
        return userRepository.findByUsernameContainingIgnoreCase(query, pageable)
            .collectList()
            .zipWith(userRepository.countByUsernameContainingIgnoreCase(query))
            .map { org.springframework.data.domain.PageImpl(it.t1, pageable, it.t2) }
    }

    fun sendFriendRequest(userId: String, friendId: String): Mono<Friendship> {
        if (userId == friendId) return Mono.error(org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Cannot friend yourself"))
        
        return userRepository.findById(friendId)
            .switchIfEmpty(Mono.error(org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "User not found")))
            .flatMap { friend ->
                val status = if (friend.isBot) FriendshipStatus.ACCEPTED else FriendshipStatus.PENDING
                val friendship = Friendship(userId = userId, friendId = friendId, status = status)
                friendshipRepository.save(friendship)
                    .flatMap { saved ->
                        if (friend.isBot) {
                            val reciprocal = Friendship(userId = friendId, friendId = userId, status = FriendshipStatus.ACCEPTED)
                            friendshipRepository.save(reciprocal).thenReturn(saved)
                        } else {
                            Mono.just(saved)
                        }
                    }
            }
    }

    fun acceptFriendRequest(userId: String, friendId: String): Mono<Friendship> {
        return friendshipRepository.findByUserIdAndFriendId(friendId, userId)
            .switchIfEmpty(Mono.error(org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Friend request not found")))
            .flatMap { 
                friendshipRepository.save(it.copy(status = FriendshipStatus.ACCEPTED))
            }
    }

    fun getFriends(userId: String): Flux<User> {
        return Flux.concat(
            friendshipRepository.findByUserId(userId)
                .filter { it.status == FriendshipStatus.ACCEPTED }
                .map { it.friendId },
            friendshipRepository.findByFriendId(userId)
                .filter { it.status == FriendshipStatus.ACCEPTED }
                .map { it.userId }
        ).distinct()
            .flatMap { userRepository.findById(it) }
    }

    fun getPendingRequests(userId: String): Flux<User> {
        return friendshipRepository.findByUserId(userId)
            .filter { it.status == FriendshipStatus.PENDING }
            .map { it.friendId }
            .flatMap { userRepository.findById(it) }
    }

    fun deleteFriend(userId: String, friendId: String): Mono<Void> {
        return friendshipRepository.findByUserIdAndFriendId(userId, friendId)
            .switchIfEmpty(friendshipRepository.findByUserIdAndFriendId(friendId, userId))
            .flatMap { friendshipRepository.delete(it) }
    }

    fun register(user: User): Mono<User> {
        val username = user.username ?: throw RuntimeException("Username is required")
        return userRepository.findByUsername(username)
            .flatMap { existingUser -> 
                Mono.error<User>(RuntimeException("User already exists")) 
            }
            .switchIfEmpty(
                Mono.defer {
                    val rawPassword = user.password ?: throw RuntimeException("Password is required")
                    val encodedPassword = passwordEncoder.encode(rawPassword)
                    
                    val encryptedEmail = user.email?.let { encryptionUtils.encrypt(it) }
                    val emailHash = user.email?.let { encryptionUtils.hash(it) }
                    
                    userRepository.save(
                        user.copy(
                            password = encodedPassword!!,
                            email = encryptedEmail,
                            emailHash = emailHash
                        )
                    )
                    .map { decryptUser(it) }
                    .onErrorResume { e ->
                        if (e is org.springframework.dao.DuplicateKeyException) {
                            Mono.error(RuntimeException("Email already exists"))
                        } else {
                            Mono.error(e)
                        }
                    }
                }
            )
    }

    fun findById(userId: String): Mono<User> {
        return userRepository.findById(userId)
            .map { decryptUser(it) }
    }

    fun findByUsername(username: String): Mono<User> {
        return userRepository.findByUsername(username)
            .map { decryptUser(it) }
    }

    fun updateProfile(userId: String, displayName: String?, bio: String?, socialLinks: Map<String, String>? = null): Mono<User> {
        return userRepository.findById(userId)
            .flatMap { user ->
                val newDisplayName = displayName ?: user.displayName
                val newBio = bio ?: user.bio
                val newSocialLinks = socialLinks ?: user.socialLinks
                userRepository.save(user.copy(displayName = newDisplayName, bio = newBio, socialLinks = newSocialLinks))
            }
            .map { decryptUser(it) }
    }

    fun updateBioWithFact(userId: String, category: String, value: String): Mono<User> {
        return userRepository.findById(userId)
            .flatMap { user ->
                val factLabel = when (category) {
                    "TECH_STACK" -> "Skills"
                    "INTEREST" -> "Interests"
                    "GOAL" -> "Goals"
                    "PERSONALITY_TRAIT" -> "Traits"
                    else -> category.lowercase().replaceFirstChar { it.uppercase() }
                }
                
                val factString = "$factLabel: $value"
                val currentBio = user.bio ?: ""
                
                if (currentBio.contains(factString)) {
                    Mono.just(user)
                } else {
                    val newBio = if (currentBio.isBlank()) {
                        factString
                    } else {
                        "$currentBio | $factString"
                    }
                    logger.info("Updating bio for user {} with new fact: {}", userId, factString)
                    userRepository.save(user.copy(bio = newBio))
                }
            }
            .map { decryptUser(it) }
    }

    fun findByEmail(email: String): Mono<User> {
        val emailHash = encryptionUtils.hash(email)
        return userRepository.findByEmailHash(emailHash)
            .switchIfEmpty(
                Mono.defer {
                    val legacyEncryptedEmail = encryptionUtils.encryptLegacy(email)
                    userRepository.findByEmail(legacyEncryptedEmail)
                        .flatMap { user ->
                            val updatedUser = user.copy(emailHash = emailHash)
                            userRepository.save(updatedUser)
                                .doOnNext { logger.info("Backfilled emailHash for user: ${it.id}") }
                        }
                }
            )
            .map { decryptUser(it) }
    }

    fun deleteUser(userId: String): Mono<Void> {
        return friendshipRepository.deleteAllByUserId(userId)
            .then(friendshipRepository.deleteAllByFriendId(userId))
            .then(userRepository.deleteById(userId))
    }

    fun updateRoles(userId: String, roles: List<String>): Mono<User> {
        return userRepository.findById(userId)
            .flatMap { user ->
                userRepository.save(user.copy(roles = roles))
            }
            .map { decryptUser(it) }
    }

    private fun decryptUser(user: User): User {
        val encryptedEmail = user.email ?: return user
        return try {
            val decryptedEmail = encryptionUtils.decrypt(encryptedEmail)
            user.copy(email = decryptedEmail)
        } catch (e: Exception) {
            logger.debug("New GCM decryption failed for user ${user.id}, attempting legacy fallback: ${e.message}")
            // Fallback to legacy decryption if new GCM decryption fails
            try {
                val decryptedEmail = encryptionUtils.decryptLegacy(encryptedEmail)
                user.copy(email = decryptedEmail)
            } catch (e2: Exception) {
                logger.error("Failed to decrypt email for user ${user.id}: ${e2.message}", e2)
                user.copy(email = null)
            }
        }
    }

    fun seedBots(bots: List<Pair<String, String>>): Mono<Void> {
        return Flux.fromIterable(bots)
            .flatMap { (name, role) ->
                userRepository.findByUsername(name)
                    .flatMap { existing ->
                        // Update existing user to ensure it's marked as bot
                        val updated = existing.copy(isBot = true, bio = "Official AdaptaChat $role Bot")
                        userRepository.save(updated)
                    }
                    .switchIfEmpty(
                        Mono.defer {
                            userRepository.save(
                                User(
                                    id = name,
                                    username = name,
                                    displayName = name,
                                    password = passwordEncoder.encode(UUID.randomUUID().toString()),
                                    bio = "Official AdaptaChat $role Bot",
                                    isBot = true
                                )
                            )
                        }
                    )
                    .flatMap { 
                        presenceTracker.setStatus(it.id!!, PresenceStatus.ONLINE, true)
                            .thenReturn(it)
                    }
            }
            .then()
    }
}
