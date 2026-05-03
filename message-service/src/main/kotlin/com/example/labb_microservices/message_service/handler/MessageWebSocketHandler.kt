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
    private val userStatusCache = ConcurrentHashMap<String, Mono<com.example.labb_microservices.proto.UserResponse>>()

    override fun handle(session: WebSocketSession): Mono<Void> {
        val token = extractToken(session)

        return session.handshakeInfo.principal
            .map { it.name }
            .switchIfEmpty(
                Mono.defer {
                    if (token != null && jwtTokenValidator.validateToken(token)) {
                        val auth = jwtTokenValidator.getAuthentication(token)
                        if (auth != null) Mono.just(auth) else Mono.empty()
                    } else {
                        Mono.empty()
                    }
                }
            )
            .flatMap { userId ->
                val sink = userSinks.computeIfAbsent(userId) {
                    Sinks.many().multicast().directBestEffort()
                }

                val output = session.send(sink.asFlux().map { session.textMessage(it) })
                    .doFinally {
                        if (sink.currentSubscriberCount() == 0) {
                            userSinks.remove(userId, sink)
                        }
                    }
                
                val validation = Flux.interval(Duration.ofSeconds(10))
                    .flatMap {
                        if (token != null && !jwtTokenValidator.validateToken(token)) {
                            Mono.error<Void>(PolicyViolationException("Token invalid"))
                        } else {
                            checkUserStatus(userId)
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
            .switchIfEmpty(
                session.close(CloseStatus(1008, "Unauthorized"))
            )
    }

    private fun checkUserStatus(userId: String): Mono<Void> {
        return userStatusCache.computeIfAbsent(userId) { id ->
            userGrpcClient.getUser(id)
                .cache(Duration.ofMinutes(1))
        }
        .flatMap { response ->
            if (!response.enabled) {
                Mono.error<Void>(PolicyViolationException("User account is disabled"))
            } else {
                Mono.empty<Void>()
            }
        }
        .onErrorResume { e ->
            userStatusCache.remove(userId)
            if (e is PolicyViolationException) {
                Mono.error<Void>(e)
            } else {
                Mono.error<Void>(PolicyViolationException("User status check failed"))
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

    fun broadcastMessage(message: String) {
        userSinks.values.forEach { it.tryEmitNext(message) }
    }

    fun sendMessageToUser(userId: String, message: String) {
        userSinks[userId]?.tryEmitNext(message)
    }

    private class PolicyViolationException(message: String) : RuntimeException(message)
}
