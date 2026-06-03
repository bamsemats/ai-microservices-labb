package com.example.labb_microservices.gateway.config

import com.example.labb_microservices.gateway.filter.JwtAuthenticationFilter
import org.springframework.beans.factory.annotation.Value
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter
import org.springframework.cloud.gateway.route.RouteLocator
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus

@Configuration
class GatewayConfig(
    private val jwtFilter: JwtAuthenticationFilter,
    private val userKeyResolver: KeyResolver,
    private val authRateLimiter: RedisRateLimiter,
    private val messageRateLimiter: RedisRateLimiter,
    @param:Value("\${services.auth:http://localhost:8081}") private val authServiceUrl: String,
    @param:Value("\${services.user:http://localhost:8082}") private val userServiceUrl: String,
    @param:Value("\${services.message:http://localhost:8083}") private val messageServiceUrl: String,
    @param:Value("\${services.aggregator:http://localhost:8086}") private val aggregatorServiceUrl: String,
    @param:Value("\${services.feedback:http://localhost:8087}") private val feedbackServiceUrl: String
) {

    @Bean
    fun customRouteLocator(builder: RouteLocatorBuilder): RouteLocator {
        return builder.routes()
            .route("auth-service") { r ->
                r.path("/login", "/refresh", "/logout")
                    .filters { f -> 
                        f.secureHeaders()
                        f.retry { it.setRetries(3).setStatuses(HttpStatus.INTERNAL_SERVER_ERROR, HttpStatus.BAD_GATEWAY, HttpStatus.SERVICE_UNAVAILABLE, HttpStatus.GATEWAY_TIMEOUT) }
                        f.requestRateLimiter { it.setRateLimiter(authRateLimiter).setKeyResolver(userKeyResolver) }
                    }
                    .uri(authServiceUrl)
            }
            .route("user-service") { r ->
                r.path("/register", "/users/**", "/friends/**", "/events/**")
                    .filters { f -> 
                        f.secureHeaders()
                        f.retry { it.setRetries(3).setStatuses(HttpStatus.INTERNAL_SERVER_ERROR, HttpStatus.BAD_GATEWAY, HttpStatus.SERVICE_UNAVAILABLE, HttpStatus.GATEWAY_TIMEOUT) }
                        f.filter(jwtFilter.apply(JwtAuthenticationFilter.Config())) 
                        f.requestRateLimiter { it.setRateLimiter(authRateLimiter).setKeyResolver(userKeyResolver) }
                    }
                    .uri(userServiceUrl)
            }
            .route("message-service") { r ->
                r.path("/messages/**", "/ws/**", "/frequencies/**")
                    .filters { f -> 
                        f.secureHeaders()
                        f.retry { it.setRetries(3).setStatuses(HttpStatus.INTERNAL_SERVER_ERROR, HttpStatus.BAD_GATEWAY, HttpStatus.SERVICE_UNAVAILABLE, HttpStatus.GATEWAY_TIMEOUT) }
                        f.filter(jwtFilter.apply(JwtAuthenticationFilter.Config())) 
                        f.requestRateLimiter { it.setRateLimiter(messageRateLimiter).setKeyResolver(userKeyResolver) }
                    }
                    .uri(messageServiceUrl)
            }
            .route("content-aggregator-service") { r ->
                r.path("/analytics/**")
                    .filters { f -> 
                        f.secureHeaders()
                        f.retry { it.setRetries(3).setStatuses(HttpStatus.INTERNAL_SERVER_ERROR, HttpStatus.BAD_GATEWAY, HttpStatus.SERVICE_UNAVAILABLE, HttpStatus.GATEWAY_TIMEOUT) }
                        f.filter(jwtFilter.apply(JwtAuthenticationFilter.Config())) 
                    }
                    .uri(aggregatorServiceUrl)
            }
            .route("feedback-service") { r ->
                r.path("/feedback/**")
                    .filters { f -> 
                        f.secureHeaders()
                        f.retry { it.setRetries(3).setStatuses(HttpStatus.INTERNAL_SERVER_ERROR, HttpStatus.BAD_GATEWAY, HttpStatus.SERVICE_UNAVAILABLE, HttpStatus.GATEWAY_TIMEOUT) }
                        f.filter(jwtFilter.apply(JwtAuthenticationFilter.Config())) 
                    }
                    .uri(feedbackServiceUrl)
            }
            .build()
    }
}
