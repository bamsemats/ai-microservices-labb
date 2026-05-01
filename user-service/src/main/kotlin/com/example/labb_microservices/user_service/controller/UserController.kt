package com.example.labb_microservices.user_service.controller

import com.example.labb_microservices.user_service.model.User
import com.example.labb_microservices.user_service.service.UserService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono

@RestController
@RequestMapping
class UserController(private val userService: UserService) {

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    fun register(@RequestBody user: User): Mono<User> {
        return userService.register(user)
    }
}
