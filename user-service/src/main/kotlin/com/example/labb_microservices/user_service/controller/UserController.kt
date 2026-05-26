package com.example.labb_microservices.user_service.controller

import com.example.labb_microservices.user_service.dto.UserDto
import com.example.labb_microservices.user_service.model.User
import com.example.labb_microservices.user_service.service.UserService
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono
import reactor.core.publisher.Flux
import org.springframework.web.server.ResponseStatusException
import jakarta.validation.Valid
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

    @DeleteMapping("/users/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteMe(@AuthenticationPrincipal userId: String): Mono<Void> {
        return userService.deleteUser(userId)
    }

    @PutMapping("/users/{id}/roles")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    fun updateRoles(@PathVariable id: String, @RequestBody roles: List<String>): Mono<UserDto> {
        if (roles.isEmpty()) {
            return Mono.error(ResponseStatusException(HttpStatus.BAD_REQUEST, "Roles list cannot be empty"))
        }
        val rolePattern = Regex("^ROLE_[A-Z0-9_]+$")
        if (roles.any { !it.trim().matches(rolePattern) }) {
            return Mono.error(ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid role format detected"))
        }

        return userService.updateRoles(id, roles.map { it.trim() })
            .map { it.toUserDto() }
    }

    @GetMapping("/users/search")
    fun searchUsers(
        @RequestParam query: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
        @AuthenticationPrincipal userId: String
    ): Mono<org.springframework.data.domain.Page<UserDto>> {
        return userService.searchUsers(query, page, size)
            .map { pageObj -> 
                pageObj.map { it.toUserDto() }
            }
    }

    @PostMapping("/friends/request/{friendId}")
    fun sendFriendRequest(
        @PathVariable friendId: String,
        @AuthenticationPrincipal userId: String
    ): Mono<UserDto> {
        return userService.sendFriendRequest(userId, friendId)
            .flatMap { userService.findById(friendId) }
            .map { it.toUserDto() }
    }

    @PostMapping("/friends/accept/{friendId}")
    fun acceptFriendRequest(
        @PathVariable friendId: String,
        @AuthenticationPrincipal userId: String
    ): Mono<UserDto> {
        return userService.acceptFriendRequest(userId, friendId)
            .flatMap { userService.findById(friendId) }
            .map { it.toUserDto() }
    }

    @GetMapping("/friends")
    fun getFriends(@AuthenticationPrincipal userId: String): Flux<UserDto> {
        return userService.getFriends(userId)
            .map { it.toUserDto() }
    }

    @DeleteMapping("/friends/{friendId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteFriend(
        @PathVariable friendId: String,
        @AuthenticationPrincipal userId: String
    ): Mono<Void> {
        return userService.deleteFriend(userId, friendId)
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
