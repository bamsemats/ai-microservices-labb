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
        val mongoDBContainer = MongoDBContainer("mongo:7.0")

        @Container
        @ServiceConnection
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
            .verifyComplete()

        // Should have 3 facts: TECH_STACK:React, TECH_STACK:Kotlin, INTEREST:React
        StepVerifier.create(memoryFragmentRepository.findByUserId("user123").collectList())
            .assertNext { fragments ->
                assertEquals(3, fragments.size)
                val techStack = fragments.filter { it.category == MemoryCategory.TECH_STACK }
                assertEquals(2, techStack.size)
                val interests = fragments.filter { it.category == MemoryCategory.INTEREST }
                assertEquals(1, interests.size)
                assertEquals("React", interests[0].value)
            }
            .verifyComplete()
    }

    @Test
    fun `should update existing memory fragment and average confidence`() {
        val message1 = Message(
            id = "msg1",
            senderId = "user456",
            receiverId = "all",
            channelId = "general",
            content = "React is awesome", // Avoid "I love" to keep it to 1 fact
            authorType = AuthorType.USER
        )

        val message2 = Message(
            id = "msg2",
            senderId = "user456",
            receiverId = "all",
            channelId = "general",
            content = "React is the best",
            authorType = AuthorType.USER
        )

        StepVerifier.create(memoryWorker.processMessageForMemory(message1))
            .verifyComplete()

        StepVerifier.create(memoryWorker.processMessageForMemory(message2))
            .verifyComplete()

        StepVerifier.create(memoryFragmentRepository.findByUserId("user456"))
            .assertNext { fragment ->
                assertEquals("React", fragment.value)
                assertEquals("msg2", fragment.sourceMessageId)
                // (0.95 + 0.95) / 2 = 0.95
                assertEquals(0.95, fragment.confidence, 0.01)
            }
            .verifyComplete()
    }

    @Test
    fun `should publish persona update event for high confidence facts`() {
        val message = Message(
            id = UUID.randomUUID().toString(),
            senderId = "user-persona",
            receiverId = "all",
            channelId = "general",
            content = "I am a React expert", // SimulatedFactExtractor gives 0.95 for React
            authorType = AuthorType.USER
        )

        StepVerifier.create(memoryWorker.processMessageForMemory(message))
            .verifyComplete()

        // Give it a moment to reach the queue
        val event = rabbitTemplate.receiveAndConvert("test.persona.queue", 5000) as? com.example.labb_microservices.ai_service.model.PersonaUpdateEvent

        assertEquals("user-persona", event?.userId)
        assertEquals("React", event?.value)
        assertEquals(MemoryCategory.TECH_STACK, event?.category)
    }
}
