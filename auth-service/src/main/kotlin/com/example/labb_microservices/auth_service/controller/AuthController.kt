package com.example.labb_microservices.auth_service.controller

import com.example.labb_microservices.auth_service.client.UserGrpcClient
import com.example.labb_microservices.auth_service.service.JwtService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

data class LoginRequest(val username: String, val password: String)
data class LoginResponse(val token: String, val userId: String, val username: String)

@RestController
@RequestMapping
class AuthController(
    private val userGrpcClient: UserGrpcClient,
    private val jwtService: JwtService
) {

    @PostMapping("/login")
    fun login(@RequestBody request: LoginRequest): Mono<ResponseEntity<LoginResponse>> {
        return Mono.fromCallable {
            val response = userGrpcClient.validateCredentials(request.username, request.password)
            if (response.valid) {
                val token = jwtService.generateToken(response.username, response.userId)
                ResponseEntity.ok(LoginResponse(token, response.userId, response.username))
            } else {
                ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
            }
        }
    }
}
