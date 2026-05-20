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
    private val logger = org.slf4j.LoggerFactory.getLogger(JwtAuthenticationFilter::class.java)

    @PostConstruct
    fun init() {
        if (secret.isBlank()) {
            throw IllegalStateException("JWT secret cannot be blank")
        }
        key = Keys.hmacShaKeyFor(secret.toByteArray())
    }

    class Config

    private val publicPaths = setOf("/login", "/register", "/refresh", "/logout")

    override fun apply(config: Config): GatewayFilter {
        return GatewayFilter { exchange, chain ->
            val request = exchange.request
            val path = request.uri.path

            logger.info("Gateway filtering path: {}", path)

            if (publicPaths.any { path == it || path.startsWith("$it/") }) {
                logger.info("Path is public: {}", path)
                return@GatewayFilter chain.filter(exchange)
            }

            val authHeader = request.headers.getFirst(HttpHeaders.AUTHORIZATION)
            var token = if (authHeader != null && authHeader.startsWith("Bearer ")) {
                authHeader.substring(7)
            } else {
                null
            }

            // Fallback to query parameter only for WebSocket handshake
            if (token == null && (path == "/ws/messages" || path.startsWith("/ws/"))) {
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

            // For WebSocket routes, ensure the Bearer token is added to the headers 
            // so downstream services (message-service) can find it via extractToken()
            if (path == "/ws/messages" || path.startsWith("/ws/")) {
                val newUri = org.springframework.web.util.UriComponentsBuilder.fromUri(request.uri)
                    .replaceQueryParam("token", null)
                    .build()
                    .toUri()

                val mutatedRequest = request.mutate()
                    .uri(newUri)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                    .build()
                return@GatewayFilter chain.filter(exchange.mutate().request(mutatedRequest).build())
            }

            // Convert query param token to Authorization header for non-WS routes
            if (request.queryParams.containsKey("token") && !path.startsWith("/ws/")) {
                val newUri = org.springframework.web.util.UriComponentsBuilder.fromUri(request.uri)
                    .replaceQueryParam("token", null)
                    .build()
                    .toUri()
                
                val mutatedRequest = request.mutate()
                    .uri(newUri)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                    .build()
                return@GatewayFilter chain.filter(exchange.mutate().request(mutatedRequest).build())
            }

            chain.filter(exchange)
        }
    }

    private fun onError(exchange: ServerWebExchange, err: String, httpStatus: HttpStatus): Mono<Void> {
        logger.error("Authentication failed for path {}: {}", exchange.request.uri.path, err)
        val response = exchange.response
        response.statusCode = httpStatus
        return response.setComplete()
    }
}
