package com.example.labb_microservices.common.security

import org.springframework.security.config.web.server.ServerHttpSecurity

object AuthorizeExchangeHelper {
    fun ServerHttpSecurity.AuthorizeExchangeSpec.authorizeActuator(): ServerHttpSecurity.AuthorizeExchangeSpec {
        return this
            .pathMatchers("/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()
            .pathMatchers("/actuator/**").authenticated()
    }
}
