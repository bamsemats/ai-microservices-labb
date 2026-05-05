package com.example.labb_microservices.user_service

import com.example.labb_microservices.user_service.config.RabbitConfig
import com.example.labb_microservices.user_service.dto.PersonaUpdateEvent
import com.example.labb_microservices.user_service.model.User
import com.example.labb_microservices.user_service.repository.UserRepository
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilAsserted
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.testcontainers.containers.MongoDBContainer
import org.testcontainers.containers.RabbitMQContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.Duration

@SpringBootTest(properties = [
    "jwt.secret=a-very-long-and-secure-secret-key-that-is-at-least-256-bits",
    "encryption.secret=another-very-long-and-secure-secret-key-32-chars",
    "grpc.server.port=0",
    "grpc.server.security.enabled=false",
    "grpc.server.security.key-store-password=password",
    "grpc.server.security.key-password=password",
    "grpc.server.security.trust-store-password=password"
])
@Testcontainers
class PersonaUpdateIntegrationTest {

    companion object {
        @Container
        @ServiceConnection
        @JvmStatic
        val mongo = MongoDBContainer("mongo:7.0")

        @Container
        @ServiceConnection
        @JvmStatic
        val rabbit = RabbitMQContainer("rabbitmq:3.12-management")
    }

    @Autowired
    private lateinit var rabbitTemplate: RabbitTemplate

    @Autowired
    private lateinit var userRepository: UserRepository

    @Test
    fun `should update user bio when persona update event is received`() {
        val user = User(
            id = "user-persona-123",
            username = "personauser",
            password = "password",
            bio = "Initial bio"
        )
        userRepository.save(user).block()

        val event = PersonaUpdateEvent(
            userId = "user-persona-123",
            category = "TECH_STACK",
            value = "Kotlin"
        )

        rabbitTemplate.convertAndSend("chat.persona.exchange", "persona.update", event)

        await.atMost(Duration.ofSeconds(10)).untilAsserted {
            val updatedUser = userRepository.findById("user-persona-123").block()
            assertTrue(updatedUser?.bio?.contains("Skills: Kotlin") == true, "Bio should have been updated with Skills: Kotlin")
            assertTrue(updatedUser?.bio?.contains("Initial bio") == true, "Original bio should be preserved")
        }
    }
}
