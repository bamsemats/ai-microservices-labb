package com.example.labb_microservices.content_aggregator

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class ContentAggregatorApplication

fun main(args: Array<String>) {
    runApplication<ContentAggregatorApplication>(*args)
}
