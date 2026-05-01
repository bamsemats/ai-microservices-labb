package com.example.labb_microservices.message_service.handler

import org.springframework.stereotype.Component
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.Mono
import reactor.core.publisher.Sinks
import java.util.concurrent.ConcurrentHashMap

@Component
class MessageWebSocketHandler : WebSocketHandler {

    private val userSinks = ConcurrentHashMap<String, Sinks.Many<String>>()

    override fun handle(session: WebSocketSession): Mono<Void> {
        return session.handshakeInfo.principal
            .map { it.name }
            .defaultIfEmpty("anonymous")
            .flatMap { userId ->
                val sink = userSinks.computeIfAbsent(userId) {
                    Sinks.many().multicast().directBestEffort()
                }

                val output = session.send(sink.asFlux().map { session.textMessage(it) })
                
                val input = session.receive()
                    .doOnTerminate {
                        // Should clean up sink if no more sessions, but simplified for now
                    }
                    .then()

                Mono.zip(input, output).then()
            }
    }

    fun sendMessageToUser(userId: String, message: String) {
        userSinks[userId]?.tryEmitNext(message)
    }
}
