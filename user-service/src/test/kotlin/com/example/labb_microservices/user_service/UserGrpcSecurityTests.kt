package com.example.labb_microservices.user_service

import com.example.labb_microservices.proto.CredentialsRequest
import com.example.labb_microservices.proto.UserServiceGrpc
import io.grpc.StatusRuntimeException
import net.devh.boot.grpc.client.inject.GrpcClient
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.MongoDBContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.nio.file.Paths

@SpringBootTest(properties = [
    "jwt.secret=a-very-long-and-secure-secret-key-that-is-at-least-256-bits",
    "encryption.secret=another-very-long-and-secure-secret-key-32-chars",
    "grpc.server.port=0"
])
@Testcontainers
@DirtiesContext
class UserGrpcSecurityTests {

    companion object {
        @Container
        @ServiceConnection
        val mongoDBContainer = MongoDBContainer("mongo:7.0")

        @JvmStatic
        @DynamicPropertySource
        fun registerProperties(registry: DynamicPropertyRegistry) {
            val certsDir = findCertsDir(java.nio.file.Paths.get(".").toAbsolutePath())
            val trustStorePath = "file:${certsDir.resolve("truststore.jks")}"
            val userServiceKeystorePath = "file:${certsDir.resolve("user-service.jks")}"
            val authServiceKeystorePath = "file:${certsDir.resolve("auth-service.jks")}"

            // Server Security
            registry.add("grpc.server.security.enabled") { "true" }
            registry.add("grpc.server.security.key-store") { userServiceKeystorePath }
            registry.add("grpc.server.security.key-store-format") { "PKCS12" }
            registry.add("grpc.server.security.key-store-password") { "password" }
            registry.add("grpc.server.security.key-password") { "password" }
            registry.add("grpc.server.security.trust-store") { trustStorePath }
            registry.add("grpc.server.security.trust-store-format") { "PKCS12" }
            registry.add("grpc.server.security.trust-store-password") { "password" }
            registry.add("grpc.server.security.client-auth") { "REQUIRE" }

            // Secure Client
            registry.add("grpc.client.secure-client.address") { "static://localhost:\${local.grpc.server.port}" }
            registry.add("grpc.client.secure-client.negotiation-type") { "TLS" }
            registry.add("grpc.client.secure-client.security.enabled") { "true" }
            registry.add("grpc.client.secure-client.security.client-auth-enabled") { "true" }
            registry.add("grpc.client.secure-client.security.key-store") { authServiceKeystorePath }
            registry.add("grpc.client.secure-client.security.key-store-format") { "PKCS12" }
            registry.add("grpc.client.secure-client.security.key-store-password") { "password" }
            registry.add("grpc.client.secure-client.security.key-password") { "password" }
            registry.add("grpc.client.secure-client.security.trust-store") { trustStorePath }
            registry.add("grpc.client.secure-client.security.trust-store-format") { "PKCS12" }
            registry.add("grpc.client.secure-client.security.trust-store-password") { "password" }

            // Insecure Client
            registry.add("grpc.client.insecure-client.address") { "static://localhost:\${local.grpc.server.port}" }
            registry.add("grpc.client.insecure-client.negotiation-type") { "TLS" }
            registry.add("grpc.client.insecure-client.security.enabled") { "true" }
            registry.add("grpc.client.insecure-client.security.client-auth-enabled") { "false" }
            registry.add("grpc.client.insecure-client.security.trust-store") { trustStorePath }
            registry.add("grpc.client.insecure-client.security.trust-store-format") { "PKCS12" }
            registry.add("grpc.client.insecure-client.security.trust-store-password") { "password" }
        }

        private fun findCertsDir(start: java.nio.file.Path): java.nio.file.Path {
            var curr: java.nio.file.Path? = start
            while (curr != null) {
                val candidate = curr.resolve("certs")
                if (java.nio.file.Files.exists(candidate)) return candidate
                curr = curr.parent
            }
            throw IllegalStateException("certs directory not found in any parent of $start")
        }
    }

    @GrpcClient("secure-client")
    private lateinit var secureStub: UserServiceGrpc.UserServiceBlockingStub

    @GrpcClient("insecure-client")
    private lateinit var insecureStub: UserServiceGrpc.UserServiceBlockingStub

    @Test
    fun `should allow connection with valid client certificate`() {
        val request = CredentialsRequest.newBuilder()
            .setUsername("test")
            .setPassword("test")
            .build()
            
        val response = secureStub.validateCredentials(request)
        assertNotNull(response)
    }

    @Test
    fun `should reject connection without client certificate`() {
        val request = CredentialsRequest.newBuilder()
            .setUsername("test")
            .setPassword("test")
            .build()
            
        assertThrows(StatusRuntimeException::class.java) {
            insecureStub.validateCredentials(request)
        }
    }
}
