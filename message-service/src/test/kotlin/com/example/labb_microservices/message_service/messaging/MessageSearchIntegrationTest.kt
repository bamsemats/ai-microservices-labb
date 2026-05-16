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
    fun `should search across multiple channels`() {
        val content1 = "Message in general channel"
        val message1 = Message(
            id = UUID.randomUUID().toString(),
            senderId = "user1",
            receiverId = "all",
            channelId = "general",
            content = content1,
            timestamp = LocalDateTime.now()
        )

        val content2 = "Message in crypto channel"
        val message2 = Message(
            id = UUID.randomUUID().toString(),
            senderId = "user2",
            receiverId = "all",
            channelId = "crypto",
            content = content2,
            timestamp = LocalDateTime.now().minusMinutes(5)
        )

        // Store both messages
        messageConsumer.storeMessage(message1)
        messageConsumer.storeMessage(message2)

        // Search for "Message" across all channels
        val resultsAll = webTestClient.get()
            .uri("/messages/search?q=Message")
            .exchange()
            .expectStatus().isOk
            .expectBodyList(Message::class.java)
            .returnResult()
            .responseBody ?: emptyList()
            
        assertEquals(2, resultsAll.size, "Should find messages from both channels")

        // Search for "Message" specifically in "crypto" channel
        val resultsCrypto = webTestClient.get()
            .uri("/messages/search?q=Message&channelId=crypto")
            .exchange()
            .expectStatus().isOk
            .expectBodyList(Message::class.java)
            .returnResult()
            .responseBody ?: emptyList()
            
        assertEquals(1, resultsCrypto.size)
        assertEquals("crypto", resultsCrypto[0].channelId)

        // Search for "Message" from "user2"
        val resultsUser2 = webTestClient.get()
            .uri("/messages/search?q=Message&senderId=user2")
            .exchange()
            .expectStatus().isOk
            .expectBodyList(Message::class.java)
            .returnResult()
            .responseBody ?: emptyList()
            
        assertEquals(1, resultsUser2.size)
        assertEquals("user2", resultsUser2[0].senderId)
    }

    @Test
    @WithMockUser(username = "user1")
    fun `should not find private messages of other users in global search`() {
        val privateContent = "This is a private secret between user2 and user3"
        val privateMessage = Message(
            id = UUID.randomUUID().toString(),
            senderId = "user2",
            receiverId = "user3",
            channelId = "general",
            content = privateContent,
            timestamp = LocalDateTime.now()
        )

        messageConsumer.storeMessage(privateMessage)

        // Search for "secret" as user1
        val results = webTestClient.get()
            .uri("/messages/search?q=secret")
            .exchange()
            .expectStatus().isOk
            .expectBodyList(Message::class.java)
            .returnResult()
            .responseBody ?: emptyList()
            
        assertEquals(0, results.size, "user1 should not be able to find private messages between others")
    }

    @Test
    @WithMockUser(username = "user1")
    fun `should find own private messages in global search`() {
        val privateContent = "My own private secret"
        val privateMessage = Message(
            id = UUID.randomUUID().toString(),
            senderId = "user1",
            receiverId = "user2",
            channelId = "general",
            content = privateContent,
            timestamp = LocalDateTime.now()
        )

        messageConsumer.storeMessage(privateMessage)

        // Search for "secret" as user1
        val results = webTestClient.get()
            .uri("/messages/search?q=secret")
            .exchange()
            .expectStatus().isOk
            .expectBodyList(Message::class.java)
            .returnResult()
            .responseBody ?: emptyList()
            
        assertEquals(1, results.size, "user1 should find their own sent private messages")
    }
}
