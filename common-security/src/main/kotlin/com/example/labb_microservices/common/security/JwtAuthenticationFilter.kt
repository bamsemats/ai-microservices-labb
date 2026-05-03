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

    @Bean
    fun userDetailsService(): MapReactiveUserDetailsService {
        // Dummy user details service to silence the generated password warning.
        // We use custom JWT filter for actual authentication.
        return MapReactiveUserDetailsService(
            User.withUsername("dummy").password("{noop}dummy").roles("USER").build()
        )
    }

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        val request = exchange.request
        val path = request.uri.path
        val authHeader = request.headers.getFirst(HttpHeaders.AUTHORIZATION)
        val tokenParam = request.queryParams.getFirst("token")

        val token = if (authHeader != null && authHeader.startsWith("Bearer ")) {
            authHeader.substring(7)
        } else if (tokenParam != null) {
            tokenParam
        } else {
            null
        }

        if (token != null && jwtTokenValidator.validateToken(token)) {
            val username = jwtTokenValidator.getAuthentication(token)
            if (username != null) {
                logger.info("Setting security context for user: {} on path: {}", username, path)
                val auth = UsernamePasswordAuthenticationToken(username, null, emptyList())
                return chain.filter(exchange)
                    .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth))
            }
        }

        logger.info("No valid token found for path: {}", path)
        return chain.filter(exchange)
    }
}
