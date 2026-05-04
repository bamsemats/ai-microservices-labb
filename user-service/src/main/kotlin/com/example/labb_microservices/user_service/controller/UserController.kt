package com.example.labb_microservices.user_service.controller

import com.example.labb_microservices.user_service.model.User
import com.example.labb_microservices.user_service.service.UserService
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono
import java.security.Principal

data class ProfileRequest(val displayName: String?, val bio: String?)

@RestController
@RequestMapping
class UserController(private val userService: UserService) {

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    fun register(@RequestBody user: User): Mono<User> {
        return userService.register(user)
    }

    @GetMapping("/users/me")
    fun me(@AuthenticationPrincipal principal: Principal): Mono<User> {
        return userService.findById(principal.name)
    }

    @PutMapping("/users/profile")
    fun updateProfile(
        @RequestBody request: ProfileRequest,
        @AuthenticationPrincipal principal: Principal
    ): Mono<User> {
        return userService.updateProfile(principal.name, request.displayName, request.bio)
    }
}
