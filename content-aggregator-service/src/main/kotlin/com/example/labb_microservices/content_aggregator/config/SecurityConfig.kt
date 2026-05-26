package com.example.labb_microservices.content_aggregator.config

import com.example.labb_microservices.common.security.JwtAuthenticationFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.SecurityWebFiltersOrder
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.web.server.SecurityWebFilterChain

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
@ComponentScan("com.example.labb_microservices.common.security")
class SecurityConfig(private val jwtAuthenticationFilter: JwtAuthenticationFilter) {

    private val logger = org.slf4j.LoggerFactory.getLogger(SecurityConfig::class.java)

    @Bean
    fun springSecurityFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        logger.info("Initializing Content Aggregator Security Filter Chain...")
        return http
            .csrf { it.disable() }
            .authorizeExchange { it
                .pathMatchers("/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()
                .pathMatchers("/analytics/trending-channels").permitAll()
                .anyExchange().authenticated()
            }
            .addFilterAt(jwtAuthenticationFilter, SecurityWebFiltersOrder.AUTHENTICATION)
            .build()
    }
}
