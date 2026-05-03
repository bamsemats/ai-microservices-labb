package com.example.labb_microservices.common.security

import org.springframework.context.annotation.Bean
import org.springframework.http.HttpHeaders
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService
import org.springframework.security.core.userdetails.User
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

@Component
class JwtAuthenticationFilter(private val jwtTokenValidator: JwtTokenValidator) : WebFilter {

    private val logger = org.slf4j.LoggerFactory.getLogger(JwtAuthenticationFilter::class.java)

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        val request = exchange.request
        val path = request.uri.path
        val authHeader = request.headers.getFirst(HttpHeaders.AUTHORIZATION)
        val tokenParam = request.queryParams.getFirst("token")
        val isWebSocketHandshake = request.headers.getFirst("Upgrade")?.equals("websocket", ignoreCase = true) == true || path.contains("/ws")

        val token = if (authHeader != null && authHeader.startsWith("Bearer ")) {
            authHeader.substring(7)
        } else if (tokenParam != null && isWebSocketHandshake) {
            tokenParam
        } else {
            null
        }

        if (token != null && jwtTokenValidator.validateToken(token)) {
            val username = jwtTokenValidator.getAuthentication(token)
            val userId = jwtTokenValidator.getUserIdFromToken(token)
            if (userId != null) {
                logger.debug("Setting security context for path: {} with userId: {}", path, userId)
                val roles = jwtTokenValidator.getRolesFromToken(token)
                val authorities = roles.map { org.springframework.security.core.authority.SimpleGrantedAuthority(it) }
                val auth = UsernamePasswordAuthenticationToken(userId, null, authorities)
                return chain.filter(exchange)
                    .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth))
            }
        }

        logger.debug("No valid token found for path: {}", path)
        return chain.filter(exchange)
    }
}
