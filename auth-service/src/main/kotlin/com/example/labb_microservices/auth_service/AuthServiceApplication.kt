package com.example.labb_microservices.auth_service

import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.core.env.Environment

@SpringBootApplication
class AuthServiceApplication {
    @Bean
    fun logProperties(env: Environment) = CommandLineRunner {
        val logger = org.slf4j.LoggerFactory.getLogger(AuthServiceApplication::class.java)
        logger.info("gRPC Client Address: {}", env.getProperty("grpc.client.user-service.address"))
        logger.info("gRPC Client Negotiation: {}", env.getProperty("grpc.client.user-service.negotiation-type"))
        logger.info("gRPC Client Security Enabled: {}", env.getProperty("grpc.client.user-service.security.enabled"))
        logger.info("gRPC Client Key Store: {}", env.getProperty("grpc.client.user-service.security.key-store"))
    }
}

fun main(args: Array<String>) {
    runApplication<AuthServiceApplication>(*args)
}
