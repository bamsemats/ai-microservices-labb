package com.example.labb_microservices.gateway.config

import com.example.labb_microservices.gateway.filter.JwtAuthenticationFilter
import org.springframework.cloud.gateway.route.RouteLocator
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class GatewayConfig(private val jwtFilter: JwtAuthenticationFilter) {

    @Bean
    fun customRouteLocator(builder: RouteLocatorBuilder): RouteLocator {
        return builder.routes()
            .route("auth-service") { r ->
                r.path("/login", "/register")
                    .uri("http://auth-service:8080") // Fallback, override via env
            }
            .route("user-service") { r ->
                r.path("/**")
                    .filters { f -> f.filter(jwtFilter.apply(JwtAuthenticationFilter.Config())) }
                    .uri("http://user-service:8080") // Fallback, override via env
            }
            .build()
    }
}
