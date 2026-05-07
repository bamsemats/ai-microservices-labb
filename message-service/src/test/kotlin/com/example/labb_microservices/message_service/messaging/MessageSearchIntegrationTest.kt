package com.example.labb_microservices.message_service.messaging

import com.example.common.test.BaseIntegrationTest
import com.example.labb_microservices.common.security.EncryptionUtils
import com.example.labb_microservices.message_service.model.Message
import com.example.labb_microservices.message_service.repository.MessageRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.reactive.server.WebTestClient
import java.time.LocalDateTime
import java.util.*

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = [
    "jwt.secret=a-very-long-and-secure-secret-key-that-is-at-least-256-bits",
    "encryption.secret=another-very-long-and-secure-secret-key-32-chars",
    "grpc.server.port=-1"
])
@AutoConfigureWebTestClient
class MessageSearchIntegrationTest : BaseIntegrationTest() {

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @Autowired
    private lateinit var messageRepository: MessageRepository

    @Autowired
    private lateinit var messageConsumer: MessageConsumer

    @Autowired
    private lateinit var encryptionUtils: EncryptionUtils

    @BeforeEach
    fun setup() {
        messageRepository.deleteAll().block()
    }

    @Test
    @WithMockUser(username = "user1")
    fun `should encrypt message and find it via blind index search`() {
        val originalContent = "The quick brown fox jumps over the lazy dog"
        val message = Message(
            id = UUID.randomUUID().toString(),
            senderId = "user1",
            receiverId = "user2",
            content = originalContent,
            timestamp = LocalDateTime.now()
        )

        // Simulate message arrival in storage queue
        messageConsumer.storeMessage(message)

        // Verify it is stored encrypted in DB
        val storedMessages = messageRepository.findAll().collectList().block() ?: emptyList()
        assertEquals(1, storedMessages.size)
        val stored = storedMessages[0]
        assertTrue(stored.content != originalContent, "Content should be encrypted")
        assertTrue(stored.searchIndices.isNotEmpty(), "Search indices should be populated")
        
        // Search for "fox"
        val resultsFox = webTestClient.get()
            .uri("/messages/search?q=fox")
            .exchange()
            .expectStatus().isOk
            .expectBodyList(Message::class.java)
            .returnResult()
            .responseBody ?: emptyList()
            
        assertEquals(1, resultsFox.size)
        assertEquals(originalContent, resultsFox[0].content, "Content should be decrypted in search results")

        // Search for "lazy"
        val resultsLazy = webTestClient.get()
            .uri("/messages/search?q=lazy")
            .exchange()
            .expectStatus().isOk
            .expectBodyList(Message::class.java)
            .returnResult()
            .responseBody ?: emptyList()
            
        assertEquals(1, resultsLazy.size)

        // Search for something non-existent
        val resultsCat = webTestClient.get()
            .uri("/messages/search?q=cat")
            .exchange()
            .expectStatus().isOk
            .expectBodyList(Message::class.java)
            .returnResult()
            .responseBody ?: emptyList()
            
        assertEquals(0, resultsCat.size)
    }

    @Test
    @WithMockUser(username = "user1")
    fun `should encrypt AI response and find it via blind index search`() {
        val originalChunk = "This is an AI response"
        val message = Message(
            id = UUID.randomUUID().toString(),
            senderId = "ai-bot",
            receiverId = "user1",
            content = originalChunk,
            authorType = com.example.labb_microservices.message_service.model.AuthorType.BOT,
            timestamp = LocalDateTime.now()
        )

        // Simulate AI response arrival
        messageConsumer.consumeAiResponse(message)

        // Verify it is stored encrypted in DB
        val storedMessages = messageRepository.findAll().collectList().block() ?: emptyList()
        assertEquals(1, storedMessages.size)
        val stored = storedMessages[0]
        assertTrue(stored.content != originalChunk, "AI content should be encrypted")
        assertTrue(stored.searchIndices.isNotEmpty(), "AI search indices should be populated")
        
        // Search for "AI"
        val resultsAi = webTestClient.get()
            .uri("/messages/search?q=AI")
            .exchange()
            .expectStatus().isOk
            .expectBodyList(Message::class.java)
            .returnResult()
            .responseBody ?: emptyList()
            
        assertEquals(1, resultsAi.size)
        assertEquals(originalChunk, resultsAi[0].content, "AI content should be decrypted in search results")
    }
}
