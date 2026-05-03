package com.example.labb_microservices.user_service.config

import com.example.labb_microservices.common.security.JwtAuthenticationFilter
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.SecurityWebFiltersOrder
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.server.SecurityWebFilterChain

@Configuration
@EnableWebFluxSecurity
@ComponentScan("com.example.labb_microservices.common.security")
class SecurityConfig(private val jwtAuthenticationFilter: JwtAuthenticationFilter) {

    @Value("\${grpc.server.security.enabled:false}")
    private var mtlsEnabled: Boolean = false

    @Value("\${grpc.server.security.key-store-password:}")
    private lateinit var keyStorePassword: String

    @Value("\${grpc.server.security.key-password:}")
    private lateinit var keyPassword: String

    @Value("\${grpc.server.security.trust-store-password:}")
    private lateinit var trustStorePassword: String

    @PostConstruct
    fun validateMtlsConfig() {
        if (mtlsEnabled) {
            if (keyStorePassword.isBlank()) throw IllegalStateException("gRPC key-store-password must be provided")
            if (keyPassword.isBlank()) throw IllegalStateException("gRPC key-password must be provided")
            if (trustStorePassword.isBlank()) throw IllegalStateException("gRPC trust-store-password must be provided")
        }
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }

    @Bean
    fun springSecurityFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        return http
            .csrf { it.disable() }
            .authorizeExchange { it
                .pathMatchers("/register").permitAll()
                .anyExchange().authenticated()
            }
            .addFilterAt(jwtAuthenticationFilter, SecurityWebFiltersOrder.AUTHENTICATION)
            .build()
    }
}
