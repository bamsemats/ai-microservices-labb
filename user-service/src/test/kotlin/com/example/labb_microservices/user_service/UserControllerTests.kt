package com.example.labb_microservices.user_service

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.ApplicationContext
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.testcontainers.containers.MongoDBContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class UserControllerTests {

    companion object {
        @Container
        @ServiceConnection
        val mongoDBContainer = MongoDBContainer("mongo:7.0")
    }

    @Autowired
    private lateinit var context: ApplicationContext

    private lateinit var webTestClient: WebTestClient

    @BeforeEach
    fun setUp() {
        webTestClient = WebTestClient.bindToApplicationContext(context).build()
    }

    @Test
    fun `should register a new user`() {
        val registrationRequest = mapOf(
            "username" to "testuser",
            "password" to "testpassword"
        )

        webTestClient.post()
            .uri("/register")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(registrationRequest)
            .exchange()
            .expectStatus().isCreated
            .expectBody()
            .jsonPath("$.username").isEqualTo("testuser")
            .jsonPath("$.id").exists()
    }
}
