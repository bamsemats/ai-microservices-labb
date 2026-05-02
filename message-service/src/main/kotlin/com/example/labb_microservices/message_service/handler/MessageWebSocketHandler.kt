package com.example.labb_microservices.message_service.handler

import com.example.labb_microservices.common.security.JwtTokenValidator
import com.example.labb_microservices.message_service.client.UserGrpcClient
import org.springframework.stereotype.Component
import org.springframework.web.reactive.socket.CloseStatus
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.Sinks
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

@Component
class MessageWebSocketHandler(
    private val jwtTokenValidator: JwtTokenValidator,
    private val userGrpcClient: UserGrpcClient
) : WebSocketHandler {

    private val userSinks = ConcurrentHashMap<String, Sinks.Many<String>>()

    override fun handle(session: WebSocketSession): Mono<Void> {
        val token = extractToken(session)

        return session.handshakeInfo.principal
            .map { it.name }
            .defaultIfEmpty("anonymous")
            .flatMap { userId ->
                val sink = userSinks.computeIfAbsent(userId) {
                    Sinks.many().multicast().directBestEffort()
                }

                val output = session.send(sink.asFlux().map { session.textMessage(it) })
                
                val validation = Flux.interval(Duration.ofSeconds(10))
                    .flatMap {
                        if (token != null && !jwtTokenValidator.validateToken(token)) {
                            Mono.error<Void>(PolicyViolationException("Token invalid"))
                        } else if (userId != "anonymous") {
                            userGrpcClient.getUser(userId)
                                .then(Mono.empty<Void>())
                                .onErrorResume {
                                    Mono.error(PolicyViolationException("User status check failed"))
                                }
                        } else {
                            Mono.empty<Void>()
                        }
                    }
                    .then()

                val input = session.receive().then()

                Mono.zip(input, output, validation)
                    .then()
                    .onErrorResume { e ->
                        if (e is PolicyViolationException) {
                            session.close(CloseStatus(1008, e.message))
                        } else {
                            session.close(CloseStatus.SERVER_ERROR)
                        }
                    }
            }
    }

    private fun extractToken(session: WebSocketSession): String? {
        val authHeader = session.handshakeInfo.headers.getFirst("Authorization")
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7)
        }
        val query = session.handshakeInfo.uri.query ?: return null
        return query.split("&")
            .find { it.startsWith("token=") }
            ?.substringAfter("token=")
    }

    fun sendMessageToUser(userId: String, message: String) {
        userSinks[userId]?.tryEmitNext(message)
    }

    private class PolicyViolationException(message: String) : RuntimeException(message)
}
