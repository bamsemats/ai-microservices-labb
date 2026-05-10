package com.example.labb_microservices.user_service.controller

import com.example.labb_microservices.user_service.dto.UserDto
import com.example.labb_microservices.user_service.model.User
import com.example.labb_microservices.user_service.service.UserService
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono
import org.springframework.web.server.ResponseStatusException
import jakarta.validation.Valid
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class ProfileRequest(
    @field:Size(max = 50)
    val displayName: String?,
    @field:Size(max = 500)
    val bio: String?,
    @field:Size(max = 10)
    val socialLinks: Map<String, String>? = null
)

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
        @Valid @RequestBody request: ProfileRequest,
        @AuthenticationPrincipal userId: String
    ): Mono<UserDto> {
        return userService.updateProfile(userId, request.displayName, request.bio, request.socialLinks)
            .map { it.toUserDto() }
            .switchIfEmpty(Mono.error(ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")))
    }

    private fun User.toUserDto() = UserDto(
        id = this.id,
        username = this.username,
        email = this.email,
        enabled = this.enabled,
        displayName = this.displayName,
        bio = this.bio,
        socialLinks = this.socialLinks
    )
}
