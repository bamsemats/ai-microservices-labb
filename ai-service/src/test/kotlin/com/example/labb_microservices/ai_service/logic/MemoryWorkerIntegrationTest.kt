package com.example.labb_microservices.ai_service.logic

import com.example.labb_microservices.ai_service.model.AuthorType
import com.example.labb_microservices.ai_service.model.MemoryCategory
import com.example.labb_microservices.ai_service.model.Message
import com.example.labb_microservices.ai_service.repository.MemoryFragmentRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.testcontainers.containers.MongoDBContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import reactor.test.StepVerifier
import java.util.*
import java.time.Duration

@SpringBootTest(properties = ["openrouter.api.key=test-key"])
@Testcontainers
class MemoryWorkerIntegrationTest {

    @Autowired
    private lateinit var memoryWorker: MemoryWorker

    @Autowired
    private lateinit var memoryFragmentRepository: MemoryFragmentRepository

    @Autowired
    private lateinit var rabbitTemplate: org.springframework.amqp.rabbit.core.RabbitTemplate

    companion object {
        @Container
        @ServiceConnection
        @JvmStatic
        val mongoDBContainer = MongoDBContainer("mongo:7.0")

        @Container
        @ServiceConnection
        @JvmStatic
        val rabbitMQContainer = org.testcontainers.containers.RabbitMQContainer("rabbitmq:3.12-management")
    }

    @org.springframework.boot.test.context.TestConfiguration
    class TestConfig {
        @org.springframework.context.annotation.Bean
        fun testPersonaQueue(): org.springframework.amqp.core.Queue {
            return org.springframework.amqp.core.Queue("test.persona.queue", false)
        }

        @org.springframework.context.annotation.Bean
        fun testPersonaBinding(testPersonaQueue: org.springframework.amqp.core.Queue, personaExchange: org.springframework.amqp.core.DirectExchange): org.springframework.amqp.core.Binding {
            return org.springframework.amqp.core.BindingBuilder.bind(testPersonaQueue).to(personaExchange).with("persona.update")
        }
    }

    @Test
    fun `should extract and save memory fragments from message`() {
        val message = Message(
            id = UUID.randomUUID().toString(),
            senderId = "user123",
            receiverId = "all",
            channelId = "general",
            content = "I love React and Kotlin!",
            authorType = AuthorType.USER
        )

        StepVerifier.create(memoryWorker.processMessageForMemory(message))
            .expectComplete()
            .verify(Duration.ofSeconds(5))

        // Should have 3 facts: TECH_STACK:React, TECH_STACK:Kotlin, INTEREST:React
        StepVerifier.create(memoryFragmentRepository.findByUserId("user123").collectList())
            .assertNext { fragments ->
                assertEquals(3, fragments.size)
                val techStack = fragments.filter { it.category == MemoryCategory.TECH_STACK }
                assertEquals(2, techStack.size)
                val interests = fragments.filter { it.category == MemoryCategory.INTEREST }
                assertEquals(1, interests.size)
                assertEquals("React And Kotlin", interests[0].value)
            }
            .expectComplete()
            .verify(Duration.ofSeconds(5))
    }

    @Test
    fun `should update existing memory fragment and keep max confidence`() {
        val message1 = Message(
            id = "msg1",
            senderId = "user456",
            receiverId = "all",
            channelId = "general",
            content = "React is okay", 
            authorType = AuthorType.USER,
            metadata = mapOf("X-Confidence-Boost" to "false") // 0.95 - 0.10 = 0.85
        )

        val message2 = Message(
            id = "msg2",
            senderId = "user456",
            receiverId = "all",
            channelId = "general",
            content = "I love React", 
            authorType = AuthorType.USER,
            metadata = mapOf("X-Confidence-Boost" to "true") // 0.95 + 0.05 = 1.0
        )

        // Reset state
        memoryFragmentRepository.deleteAll().block(Duration.ofSeconds(5))

        StepVerifier.create(memoryWorker.processMessageForMemory(message1))
            .expectComplete()
            .verify(Duration.ofSeconds(5))

        StepVerifier.create(memoryWorker.processMessageForMemory(message2))
            .expectComplete()
            .verify(Duration.ofSeconds(5))

        StepVerifier.create(memoryFragmentRepository.findByUserId("user456").collectList())
            .assertNext { fragments ->
                val techStack = fragments.find { it.category == MemoryCategory.TECH_STACK }
                org.junit.jupiter.api.Assertions.assertNotNull(techStack, "Tech stack fragment should exist")
                assertEquals("React", techStack?.value)
                assertEquals("msg2", techStack?.sourceMessageId)
                // Max(0.85, 1.0) = 1.0
                assertEquals(1.0, techStack?.confidence ?: 0.0, 0.01)
            }
            .expectComplete()
            .verify(Duration.ofSeconds(5))
    }

    @Test
    fun `should publish persona update event for high confidence facts`() {
        // Drain queue
        while (rabbitTemplate.receive("test.persona.queue", 100) != null) { /* ignore */ }

        val message = Message(
            id = UUID.randomUUID().toString(),
            senderId = "user-persona",
            receiverId = "all",
            channelId = "general",
            content = "I am a React expert", // SimulatedFactExtractor gives 0.95 for React
            authorType = AuthorType.USER
        )

        StepVerifier.create(memoryWorker.processMessageForMemory(message))
            .expectComplete()
            .verify(Duration.ofSeconds(5))

        // Give it a moment to reach the queue
        val event = rabbitTemplate.receiveAndConvert("test.persona.queue", 5000) as? com.example.labb_microservices.ai_service.model.PersonaUpdateEvent

        org.junit.jupiter.api.Assertions.assertNotNull(event, "PersonaUpdateEvent should not be null")
        assertEquals("user-persona", event?.userId)
        assertEquals("React", event?.value)
        assertEquals(MemoryCategory.TECH_STACK, event?.category)
    }
}
