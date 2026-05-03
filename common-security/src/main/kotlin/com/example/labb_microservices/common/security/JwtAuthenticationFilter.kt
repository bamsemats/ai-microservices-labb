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

    @Bean
    fun userDetailsService(): MapReactiveUserDetailsService {
        // Dummy user details service to silence the generated password warning.
        // We use custom JWT filter for actual authentication.
        return MapReactiveUserDetailsService(
            User.withUsername("dummy").password("{noop}dummy").roles("USER").build()
        )
    }

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {

        val authHeader = exchange.request.headers.getFirst(HttpHeaders.AUTHORIZATION)
        var token: String? = null

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7)
        } else {
            token = exchange.request.queryParams.getFirst("token")
        }

        if (token != null) {
            val username = jwtTokenValidator.getAuthentication(token)

            if (username != null) {
                val auth = UsernamePasswordAuthenticationToken(username, null, emptyList())
                return chain.filter(exchange)
                    .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth))
            }
        }

        return chain.filter(exchange)
    }
}
