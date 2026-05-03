package com.example.labb_microservices.common.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService
import org.springframework.security.core.userdetails.User

@Configuration
class CommonSecurityConfig {

    @Bean("commonUserDetailsService")
    fun userDetailsService(): MapReactiveUserDetailsService {
        // Dummy user details service to silence the generated password warning.
        // We use custom JWT filter for actual authentication.
        return MapReactiveUserDetailsService(
            User.withUsername("dummy").password("{noop}dummy").roles("USER").build()
        )
    }
}
