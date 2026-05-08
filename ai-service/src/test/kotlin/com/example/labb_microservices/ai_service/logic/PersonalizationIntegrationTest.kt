package com.example.labb_microservices.ai_service.logic

import com.example.common.test.BaseIntegrationTest
import com.example.labb_microservices.ai_service.messaging.RabbitMQConfig
import com.example.labb_microservices.ai_service.model.AuthorType
import com.example.labb_microservices.ai_service.model.MemoryCategory
import com.example.labb_microservices.ai_service.model.MemoryFragment
import com.example.labb_microservices.ai_service.model.Message
import com.example.labb_microservices.ai_service.repository.MemoryFragmentRepository
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import reactor.test.StepVerifier
import java.time.Duration
import java.util.*
import org.slf4j.LoggerFactory

@SpringBootTest(properties = ["openrouter.api.key=test-key"])
@Import(RabbitMQConfig::class)
class PersonalizationIntegrationTest : BaseIntegrationTest() {

    private val logger = LoggerFactory.getLogger(PersonalizationIntegrationTest::class.java)

    @Autowired
    private lateinit var responseGenerator: ResponseGenerator

    @Autowired
    private lateinit var memoryFragmentRepository: MemoryFragmentRepository

    @org.junit.jupiter.api.BeforeEach
    fun setUp() {
        memoryFragmentRepository.deleteAll().block(Duration.ofSeconds(5))
    }

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
        
        memoryFragmentRepository.save(fragment).block(Duration.ofSeconds(10))

        val message = Message(
            id = UUID.randomUUID().toString(),
            senderId = userId,
            receiverId = "ai-bot",
            channelId = "general",
            content = "What do you think of this architecture?",
            authorType = AuthorType.USER,
            metadata = mapOf("X-Adapta-Test-Mode" to "true")
        )

        StepVerifier.create(responseGenerator.generateResponse(message))
            .assertNext { response ->
                logger.info("AI Response: {}", response)
                assertTrue(response.contains("React"), "Response should have mentioned React")
            }
            .expectComplete()
            .verify(Duration.ofSeconds(10))
    }
}
