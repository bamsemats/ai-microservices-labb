package com.example.labb_microservices.message_service

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class MessageServiceApplication

fun main(args: Array<String>) {
    runApplication<MessageServiceApplication>(*args)
}
