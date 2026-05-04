package com.example.labb_microservices.user_service.controller

import com.example.labb_microservices.user_service.dto.UserDto
import com.example.labb_microservices.user_service.model.User
import com.example.labb_microservices.user_service.service.UserService
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono

data class ProfileRequest(val displayName: String?, val bio: String?)

@RestController
@RequestMapping
class UserController(private val userService: UserService) {

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    fun register(@RequestBody user: User): Mono<UserDto> {
        return userService.register(user).map { it.toUserDto() }
    }

    @GetMapping("/users/me")
    fun me(@AuthenticationPrincipal userId: String): Mono<UserDto> {
        return userService.findById(userId).map { it.toUserDto() }
    }

    @PutMapping("/users/profile")
    fun updateProfile(
        @RequestBody request: ProfileRequest,
        @AuthenticationPrincipal userId: String
    ): Mono<UserDto> {
        return userService.updateProfile(userId, request.displayName, request.bio).map { it.toUserDto() }
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
