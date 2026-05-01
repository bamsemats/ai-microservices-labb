package com.example.labb_microservices.user_service.service

import com.example.labb_microservices.user_service.model.User
import com.example.labb_microservices.user_service.repository.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class UserService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder
) {
    fun register(user: User): Mono<User> {
        val username = user.username ?: throw RuntimeException("Username is required")
        return userRepository.findByUsername(username)
            .flatMap { Mono.error<User>(RuntimeException("User already exists")) }
            .switchIfEmpty(
                Mono.defer {
                    val rawPassword = user.password ?: throw RuntimeException("Password is required")
                    val encodedPassword = passwordEncoder.encode(rawPassword)
                    userRepository.save(
                        user.copy(password = encodedPassword!!)
                    )
                }
            )
    }
}
