package com.example.labb_microservices.feedback_service.controller

import com.example.common.test.BaseIntegrationTest
import com.example.labb_microservices.feedback_service.model.Feedback
import com.example.labb_microservices.feedback_service.repository.FeedbackRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.reactive.server.WebTestClient
import java.util.*

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = [
    "jwt.secret=a-very-long-and-secure-secret-key-that-is-at-least-256-bits",
    "encryption.secret=another-very-long-and-secure-secret-key-32-chars"
])
@AutoConfigureWebTestClient
class FeedbackControllerIntegrationTest : BaseIntegrationTest() {

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @Autowired
    private lateinit var feedbackRepository: FeedbackRepository

    @BeforeEach
    fun setup() {
        feedbackRepository.deleteAll().block()
    }

    @Test
    @WithMockUser(username = "user-1")
    fun `should submit feedback successfully`() {
        val request = FeedbackRequest(
            rating = 5,
            comment = "Great app! Love the AI features."
        )

        webTestClient.post()
            .uri("/feedback")
            .bodyValue(request)
            .exchange()
            .expectStatus().isOk
            .expectBody(Feedback::class.java)
            .consumeWith { result ->
                val feedback = result.responseBody
                assertNotNull(feedback)
                assertEquals(5, feedback?.rating)
                assertEquals("Great app! Love the AI features.", feedback?.comment)
                assertEquals("user-1", feedback?.userId)
            }

        val count = feedbackRepository.count().block()
        assertEquals(1, count)
    }

    @Test
    @WithMockUser(username = "admin-1", roles = ["ADMIN"])
    fun `should retrieve all feedback for admin`() {
        val f1 = Feedback(userId = "u1", rating = 4, comment = "Nice")
        val f2 = Feedback(userId = "u2", rating = 2, comment = "Buggy")
        feedbackRepository.saveAll(listOf(f1, f2)).collectList().block()

        webTestClient.get()
            .uri("/feedback")
            .exchange()
            .expectStatus().isOk
            .expectBodyList(Feedback::class.java)
            .hasSize(2)
    }

    @Test
    @WithMockUser(username = "user-1", roles = ["USER"])
    fun `should deny access to get all feedback for non-admin`() {
        webTestClient.get()
            .uri("/feedback")
            .exchange()
            .expectStatus().isForbidden
    }
}
