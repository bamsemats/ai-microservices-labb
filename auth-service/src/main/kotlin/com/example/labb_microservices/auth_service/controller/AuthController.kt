package com.example.labb_microservices.auth_service.controller

import com.example.labb_microservices.auth_service.client.UserGrpcClient
import com.example.labb_microservices.auth_service.service.JwtService
import com.example.labb_microservices.auth_service.service.RefreshTokenService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers

data class LoginRequest(val username: String, val password: String)
data class RefreshRequest(val userId: String, val refreshToken: String)
data class LoginResponse(
    val accessToken: String, 
    val refreshToken: String, 
    val userId: String, 
    val username: String, 
    val role: String,
    val forcePasswordChange: Boolean = false
)
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
                    val roles = response.rolesList ?: listOf("ROLE_USER")
                    val primaryRole = roles.firstOrNull { it == "ROLE_ADMIN" } ?: roles.firstOrNull() ?: "ROLE_USER"
                    
                    val accessToken = jwtService.generateAccessToken(response.username, response.userId, roles)
                    val refreshToken = jwtService.generateRefreshToken(response.username, response.userId, roles)
                    refreshTokenService.saveRefreshToken(response.userId, refreshToken)
                        .flatMap { saved ->
                            if (saved) {
                                val cookie = org.springframework.http.ResponseCookie.from("accessToken", accessToken)
                                    .httpOnly(true)
                                    .secure(true) // Should be true in prod
                                    .path("/")
                                    .maxAge(3600)
                                    .sameSite("Strict")
                                    .build()
                                
                                Mono.just(ResponseEntity.ok()
                                    .header(org.springframework.http.HttpHeaders.SET_COOKIE, cookie.toString())
                                    .body(LoginResponse(
                                        accessToken, 
                                        refreshToken, 
                                        response.userId, 
                                        response.username, 
                                        primaryRole,
                                        response.forcePasswordChange
                                    )))
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
        if (!jwtService.validateToken(request.refreshToken, "refresh")) {
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build())
        }
        
        val claims = jwtService.getClaims(request.refreshToken)
        val username = claims?.subject ?: return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build())
        val userId = claims["userId"] as? String ?: return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build())
        @Suppress("UNCHECKED_CAST")
        val roles = (claims["roles"] as? List<*>)?.filterIsInstance<String>()?.ifEmpty { listOf("ROLE_USER") } ?: listOf("ROLE_USER")
        
        val newAccessToken = jwtService.generateAccessToken(username, userId, roles)
        val newRefreshToken = jwtService.generateRefreshToken(username, userId, roles)
        
        return refreshTokenService.rotateRefreshToken(userId, request.refreshToken, newRefreshToken)
            .flatMap { success ->
                if (success) {
                    Mono.just(ResponseEntity.ok(TokenResponse(newAccessToken, newRefreshToken)))
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
                        .flatMap { success ->
                            if (success) {
                                Mono.just(ResponseEntity.noContent().build<Void>())
                            } else {
                                Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build<Void>())
                            }
                        }
                } else {
                    Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build())
                }
            }
    }
}
