package com.example.labb_microservices.user_service.controller

import com.example.labb_microservices.user_service.dto.UserDto
import com.example.labb_microservices.user_service.model.User
import com.example.labb_microservices.user_service.service.UserService
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono

import org.springframework.web.server.ResponseStatusException

data class ProfileRequest(val displayName: String?, val bio: String?)
data class RegisterUserRequest(val username: String, val email: String, val password: String)

@RestController
@RequestMapping
class UserController(private val userService: UserService) {

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    fun register(@RequestBody request: RegisterUserRequest): Mono<UserDto> {
        val user = User(
            username = request.username,
            email = request.email,
            password = request.password
        )
        return userService.register(user).map { it.toUserDto() }
    }

    @GetMapping("/users/me")
    fun me(@AuthenticationPrincipal userId: String): Mono<UserDto> {
        return userService.findById(userId)
            .map { it.toUserDto() }
            .switchIfEmpty(Mono.error(ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")))
    }

    @PutMapping("/users/profile")
    fun updateProfile(
        @RequestBody request: ProfileRequest,
        @AuthenticationPrincipal userId: String
    ): Mono<UserDto> {
        return userService.updateProfile(userId, request.displayName, request.bio)
            .map { it.toUserDto() }
            .switchIfEmpty(Mono.error(ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")))
    }

    private fun User.toUserDto() = UserDto(
        id = this.id,
        username = this.username,
        email = this.email,
        enabled = this.enabled,
        displayName = this.displayName,
        bio = this.bio
    )
}
