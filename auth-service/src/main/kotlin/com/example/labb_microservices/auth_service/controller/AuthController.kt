package com.example.labb_microservices.auth_service.controller

import com.example.labb_microservices.auth_service.client.UserGrpcClient
import com.example.labb_microservices.auth_service.service.JwtService
import com.example.labb_microservices.auth_service.service.RefreshTokenService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.security.Principal

data class LoginRequest(val username: String, val password: String)
data class RefreshRequest(val userId: String, val refreshToken: String)
data class LoginResponse(val accessToken: String, val refreshToken: String, val userId: String, val username: String)
data class TokenResponse(val accessToken: String, val refreshToken: String)

@RestController
@RequestMapping
class AuthController(
    private val userGrpcClient: UserGrpcClient,
    private val jwtService: JwtService,
    private val refreshTokenService: RefreshTokenService
) {

    @PostMapping("/login")
    fun login(@RequestBody request: LoginRequest): Mono<ResponseEntity<LoginResponse>> {
        return userGrpcClient.validateCredentials(request.username, request.password)
            .subscribeOn(Schedulers.boundedElastic())
            .flatMap { response ->
                if (response.valid) {
                    val accessToken = jwtService.generateAccessToken(response.username, response.userId)
                    val refreshToken = jwtService.generateRefreshToken(response.username, response.userId)
                    refreshTokenService.saveRefreshToken(response.userId, refreshToken)
                        .flatMap { saved ->
                            if (saved) {
                                Mono.just(ResponseEntity.ok(LoginResponse(accessToken, refreshToken, response.userId, response.username)))
                            } else {
                                Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build())
                            }
                        }
                } else {
                    Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build())
                }
            }
    }

    @PostMapping("/refresh")
    fun refresh(@RequestBody request: RefreshRequest): Mono<ResponseEntity<TokenResponse>> {
        return refreshTokenService.validateRefreshToken(request.userId, request.refreshToken)
            .flatMap { isValid ->
                if (isValid && jwtService.validateToken(request.refreshToken, "refresh")) {
                    val claims = jwtService.getClaims(request.refreshToken)
                    val username = claims?.subject ?: return@flatMap Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build())
                    
                    val newAccessToken = jwtService.generateAccessToken(username, request.userId)
                    val newRefreshToken = jwtService.generateRefreshToken(username, request.userId)
                    
                    refreshTokenService.saveRefreshToken(request.userId, newRefreshToken)
                        .flatMap { saved ->
                            if (saved) {
                                Mono.just(ResponseEntity.ok(TokenResponse(newAccessToken, newRefreshToken)))
                            } else {
                                Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build())
                            }
                        }
                } else {
                    Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build())
                }
            }
    }

    @PostMapping("/logout")
    fun logout(@RequestBody request: RefreshRequest): Mono<ResponseEntity<Void>> {
        return refreshTokenService.validateRefreshToken(request.userId, request.refreshToken)
            .flatMap { isValid ->
                if (isValid && jwtService.validateToken(request.refreshToken, "refresh")) {
                    refreshTokenService.deleteRefreshToken(request.userId)
                        .map { ResponseEntity.noContent().build<Void>() }
                } else {
                    Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build())
                }
            }
    }
}
