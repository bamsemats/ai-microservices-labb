package com.example.labb_microservices.gateway.config

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

@Configuration
class SecurityHardeningConfig {

    @Bean
    fun userKeyResolver(): KeyResolver {
        return KeyResolver { exchange: ServerWebExchange ->
            // Rate limit by IP address
            Mono.just(exchange.request.remoteAddress?.address?.hostAddress ?: "anonymous")
        }
    }

    @Bean
    @org.springframework.context.annotation.Primary
    fun authRateLimiter(): RedisRateLimiter {
        // Conservative rate limit for auth endpoints: 5 requests per second, burst capacity of 10
        return RedisRateLimiter(5, 10)
    }

    @Bean
    fun messageRateLimiter(): RedisRateLimiter {
        // Slightly more relaxed rate limit for messaging: 20 requests per second, burst capacity of 40
        return RedisRateLimiter(20, 40)
    }
}
