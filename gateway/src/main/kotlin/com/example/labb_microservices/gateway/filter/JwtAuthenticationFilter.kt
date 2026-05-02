package com.example.labb_microservices.gateway.filter

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.cloud.gateway.filter.GatewayFilter
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import javax.crypto.SecretKey

@Component
class JwtAuthenticationFilter(
    @Value("\${jwt.secret}")
    private val secret: String
) : AbstractGatewayFilterFactory<JwtAuthenticationFilter.Config>(Config::class.java) {

    private lateinit var key: SecretKey

    @PostConstruct
    fun init() {
        if (secret.isBlank()) {
            throw IllegalStateException("JWT secret cannot be blank")
        }
        key = Keys.hmacShaKeyFor(secret.toByteArray())
    }

    class Config

    override fun apply(config: Config): GatewayFilter {
        return GatewayFilter { exchange, chain ->
            val request = exchange.request
            val path = request.uri.path

            if (path.contains("/login") || path.contains("/register")) {
                return@GatewayFilter chain.filter(exchange)
            }

            val authHeader = request.headers.getFirst(HttpHeaders.AUTHORIZATION)
            var token = if (authHeader != null && authHeader.startsWith("Bearer ")) {
                authHeader.substring(7)
            } else {
                null
            }

            // Fallback to query parameter only for WebSocket handshake
            if (token == null && path.startsWith("/ws/")) {
                token = request.queryParams.getFirst("token")
            }

            if (token == null) {
                return@GatewayFilter onError(exchange, "Missing authorization token", HttpStatus.UNAUTHORIZED)
            }

            try {
                val claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .payload
                if (claims["tokenType"] != "access") {
                    return@GatewayFilter onError(exchange, "Invalid token type", HttpStatus.UNAUTHORIZED)
                }
            } catch (e: Exception) {
                return@GatewayFilter onError(exchange, "Invalid token", HttpStatus.UNAUTHORIZED)
            }

            // If token was in query param, we should ideally strip it before forwarding
            // In Spring Cloud Gateway, we can mutate the exchange to remove the param
            if (request.queryParams.containsKey("token")) {
                val newUri = org.springframework.web.util.UriComponentsBuilder.fromUri(request.uri)
                    .replaceQueryParam("token", null)
                    .build()
                    .toUri()
                
                val mutatedRequest = request.mutate().uri(newUri).build()
                return@GatewayFilter chain.filter(exchange.mutate().request(mutatedRequest).build())
            }

            chain.filter(exchange)
        }
    }

    private fun onError(exchange: ServerWebExchange, err: String, httpStatus: HttpStatus): Mono<Void> {
        val response = exchange.response
        response.statusCode = httpStatus
        return response.setComplete()
    }
}
