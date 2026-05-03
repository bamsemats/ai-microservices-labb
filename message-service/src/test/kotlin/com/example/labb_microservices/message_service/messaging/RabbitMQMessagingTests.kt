package com.example.labb_microservices.message_service.messaging

import com.example.labb_microservices.message_service.client.UserGrpcClient
import com.example.labb_microservices.message_service.handler.MessageWebSocketHandler
import com.example.labb_microservices.message_service.model.Message
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
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.context.annotation.Bean
import org.springframework.amqp.core.*
import java.time.LocalDateTime

@SpringBootTest(properties = [
    "jwt.secret=a-very-long-and-secure-secret-key-that-is-at-least-256-bits",
    "encryption.secret=another-very-long-and-secure-secret-key-32-chars",
    "grpc.server.port=0"
])
@Testcontainers
@Import(RabbitMQMessagingTests.LatchConfig::class, RabbitMQMessagingTests.ListenerConfig::class)
class RabbitMQMessagingTests {

    companion object {
        @Container
        @ServiceConnection
        val mongodb = MongoDBContainer("mongo:6.0.4")

        @Container
        @ServiceConnection
        val rabbitmq = RabbitMQContainer("rabbitmq:3.11-management")
    }

    @Autowired
    @org.springframework.beans.factory.annotation.Qualifier("latch1")
    private lateinit var latch1: CountDownLatch

    @Autowired
    @org.springframework.beans.factory.annotation.Qualifier("latch2")
    private lateinit var latch2: CountDownLatch

    @Autowired
    private lateinit var messageProducer: MessageProducer

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private lateinit var userGrpcClient: UserGrpcClient

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private lateinit var webSocketHandler: MessageWebSocketHandler

    @Test
    fun `should deliver message to multiple listeners when using fanout`() {
        val message = Message(
            senderId = "user1",
            receiverId = "user2",
            content = "Hello Fanout",
            timestamp = LocalDateTime.now()
        )

        messageProducer.deliverMessage(message)

        val received1 = latch1.await(10, TimeUnit.SECONDS)
        val received2 = latch2.await(10, TimeUnit.SECONDS)

        assertTrue(received1, "Consumer 1 should have received the message")
        assertTrue(received2, "Consumer 2 should have received the message")
    }

    @TestConfiguration
    class LatchConfig {
        @Bean
        fun latch1(): CountDownLatch = CountDownLatch(1)

        @Bean
        fun latch2(): CountDownLatch = CountDownLatch(1)
    }

    @TestConfiguration
    class ListenerConfig {
        @Autowired
        private lateinit var latch1: CountDownLatch

        @Autowired
        private lateinit var latch2: CountDownLatch

        @Bean
        fun testQueue1(): Queue = Queue("test.queue.1", false, true, true)

        @Bean
        fun testQueue2(): Queue = Queue("test.queue.2", false, true, true)

        @Bean
        fun testBinding1(testQueue1: Queue, @org.springframework.beans.factory.annotation.Qualifier("deliveryExchange") exchange: FanoutExchange): Binding =
            BindingBuilder.bind(testQueue1).to(exchange)

        @Bean
        fun testBinding2(testQueue2: Queue, @org.springframework.beans.factory.annotation.Qualifier("deliveryExchange") exchange: FanoutExchange): Binding =
            BindingBuilder.bind(testQueue2).to(exchange)

        @RabbitListener(queues = ["test.queue.1"])
        fun firstConsumer(message: Message) {
            latch1.countDown()
        }

        @RabbitListener(queues = ["test.queue.2"])
        fun secondConsumer(message: Message) {
            latch2.countDown()
        }
    }
}
