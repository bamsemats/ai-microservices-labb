package com.example.labb_microservices.user_service.service

import com.example.labb_microservices.common.security.EncryptionUtils
import com.example.labb_microservices.user_service.model.User
import com.example.labb_microservices.user_service.repository.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class UserService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val encryptionUtils: EncryptionUtils
) {
    fun register(user: User): Mono<User> {
        val username = user.username ?: throw RuntimeException("Username is required")
        return userRepository.findByUsername(username)
            .flatMap { Mono.error<User>(RuntimeException("User already exists")) }
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

    fun findByEmail(email: String): Mono<User> {
        val emailHash = encryptionUtils.hash(email)
        return userRepository.findByEmailHash(emailHash)
    }
}
