package com.example.labb_microservices.gateway.config

import com.example.labb_microservices.gateway.filter.JwtAuthenticationFilter
import org.springframework.beans.factory.annotation.Value
import org.springframework.cloud.gateway.route.RouteLocator
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class GatewayConfig(
    private val jwtFilter: JwtAuthenticationFilter,
    @Value("\${services.auth:http://localhost:8081}") private val authServiceUrl: String,
    @Value("\${services.user:http://localhost:8082}") private val userServiceUrl: String,
    @Value("\${services.message:http://localhost:8083}") private val messageServiceUrl: String
) {

    @Bean
    fun customRouteLocator(builder: RouteLocatorBuilder): RouteLocator {
        return builder.routes()
            .route("auth-service") { r ->
                r.path("/login", "/refresh", "/logout")
                    .uri(authServiceUrl)
            }
            .route("user-service") { r ->
                r.path("/register", "/users/**")
                    .filters { f -> f.filter(jwtFilter.apply(JwtAuthenticationFilter.Config())) }
                    .uri(userServiceUrl)
            }
            .route("message-service") { r ->
                r.path("/messages/**", "/ws/**")
                    .filters { f -> f.filter(jwtFilter.apply(JwtAuthenticationFilter.Config())) }
                    .uri(messageServiceUrl)
            }
            .build()
    }
}
