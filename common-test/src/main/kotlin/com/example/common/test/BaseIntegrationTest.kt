package com.example.common.test

import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource

abstract class BaseIntegrationTest {
    companion object {
        @JvmStatic
        @DynamicPropertySource
        fun registerProperties(registry: DynamicPropertyRegistry) {
            SingletonContainers.startAll()
            
            registry.add("spring.data.mongodb.uri", SingletonContainers.mongoDBContainer::getReplicaSetUrl)
            
            registry.add("spring.rabbitmq.host", SingletonContainers.rabbitMQContainer::getHost)
            registry.add("spring.rabbitmq.port", SingletonContainers.rabbitMQContainer::getAmqpPort)
            registry.add("spring.rabbitmq.username", SingletonContainers.rabbitMQContainer::getAdminUsername)
            registry.add("spring.rabbitmq.password", SingletonContainers.rabbitMQContainer::getAdminPassword)
            
            registry.add("spring.data.redis.host", SingletonContainers.redisContainer::getHost)
            registry.add("spring.data.redis.port") { SingletonContainers.redisContainer.getMappedPort(6379) }
        }
    }
}
