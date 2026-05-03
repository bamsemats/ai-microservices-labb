package com.example.labb_microservices.user_service.service

import com.example.labb_microservices.common.security.EncryptionUtils
import com.example.labb_microservices.user_service.model.User
import com.example.labb_microservices.user_service.repository.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

import org.slf4j.LoggerFactory

@Service
class UserService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val encryptionUtils: EncryptionUtils
) {
    private val logger = LoggerFactory.getLogger(UserService::class.java)

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

    private fun decryptUser(user: User): User {
        val encryptedEmail = user.email ?: return user
        return try {
            val decryptedEmail = encryptionUtils.decrypt(encryptedEmail)
            user.copy(email = decryptedEmail)
        } catch (e: Exception) {
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
}
