package com.example.labb_microservices.ai_service.logic

import com.example.labb_microservices.ai_service.model.AuthorType
import com.example.labb_microservices.ai_service.model.MemoryCategory
import com.example.labb_microservices.ai_service.model.MemoryFragment
import com.example.labb_microservices.ai_service.model.Message
import com.example.labb_microservices.ai_service.repository.MemoryFragmentRepository
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
class PersonalizationIntegrationTest {

    companion object {
        @Container
        @ServiceConnection
        val mongoDBContainer = MongoDBContainer("mongo:7.0")
    }

    @Autowired
    private lateinit var responseGenerator: ResponseGenerator

    @Autowired
    private lateinit var memoryFragmentRepository: MemoryFragmentRepository

    @Test
    fun `AI should reference user interests in response`() {
        val userId = "user-789"
        
        // Pre-seed memory
        val fragment = MemoryFragment(
            userId = userId,
            category = MemoryCategory.TECH_STACK,
            value = "React",
            confidence = 0.99,
            sourceMessageId = "past-msg"
        )
        
        memoryFragmentRepository.save(fragment).block()

        val message = Message(
            id = "test-" + UUID.randomUUID().toString(),
            senderId = userId,
            receiverId = "ai-bot",
            channelId = "general",
            content = "What do you think of this architecture?",
            authorType = AuthorType.USER
        )

        StepVerifier.create(responseGenerator.generateResponse(message))
            .assertNext { response ->
                println("AI Response: $response")
                assert(response.contains("React")) { "Response should have mentioned React" }
            }
            .verifyComplete()
    }
}
