package com.example.labb_microservices.feedback_service

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = [
    "com.example.labb_microservices.feedback_service",
    "com.example.labb_microservices.common.security",
    "com.example.common.observability"
])
class FeedbackServiceApplication

fun main(args: Array<String>) {
    runApplication<FeedbackServiceApplication>(*args)
}
