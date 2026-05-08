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
        GenericContainer<Nothing>("redis:7.2")
            .apply {
                withExposedPorts(6379)
                withReuse(true)
            }
    }

    fun startAll() {
        // Enable reuse programmatically if not already set
        if (System.getProperty("testcontainers.reuse.enable") == null) {
            System.setProperty("testcontainers.reuse.enable", "true")
        }
        
        Startables.deepStart(mongoDBContainer, rabbitMQContainer, redisContainer).join()
    }
}
