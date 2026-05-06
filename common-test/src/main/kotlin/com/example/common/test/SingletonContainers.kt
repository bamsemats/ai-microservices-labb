package com.example.common.test

import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.MongoDBContainer
import org.testcontainers.containers.RabbitMQContainer
import org.testcontainers.lifecycle.Startables

object SingletonContainers {
    val mongoDBContainer: MongoDBContainer by lazy {
        MongoDBContainer("mongo:7.0")
            .withReuse(true)
    }

    val rabbitMQContainer: RabbitMQContainer by lazy {
        RabbitMQContainer("rabbitmq:3.12-management")
            .withReuse(true)
    }

    val redisContainer: GenericContainer<*> by lazy {
        GenericContainer("redis:7.2")
            .withExposedPorts(6379)
            .withReuse(true)
    }

    fun startAll() {
        Startables.deepStart(mongoDBContainer, rabbitMQContainer, redisContainer).join()
    }
}
