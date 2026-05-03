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
    private val userChannels = ConcurrentHashMap<String, String>()
    private val userStatusCache = ConcurrentHashMap<String, com.example.labb_microservices.proto.UserResponse>()

    override fun handle(session: WebSocketSession): Mono<Void> {
        val token = extractToken(session)
        val channelId = extractChannel(session) ?: "general"

        return Mono.justOrEmpty(token)
            .flatMap { t: String ->
                if (jwtTokenValidator.validateToken(t)) {
                    val userId = jwtTokenValidator.getUserIdFromToken(t)
                    if (userId != null) Mono.just(userId) else Mono.empty<String>()
                } else {
                    Mono.empty<String>()
                }
            }
            .switchIfEmpty(
                session.handshakeInfo.principal
                    .map { it.name }
            )
            .flatMap { userId: String ->
                val sink = userSinks.computeIfAbsent(userId) {
                    Sinks.many().multicast().directBestEffort()
                }
                userChannels[userId] = channelId

                val output = session.send(sink.asFlux().map { session.textMessage(it) })
                    .doFinally {
                        if (sink.currentSubscriberCount() == 0) {
                            userSinks.remove(userId, sink)
                            userChannels.remove(userId)
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
        val cached = userStatusCache[userId]
        if (cached != null) {
            // Proto3 optional fields have hasField() methods in Java
            return if (cached.enabled == false) {
                Mono.error(PolicyViolationException("User account is disabled"))
            } else {
                Mono.empty()
            }
        }

        return userGrpcClient.getUser(userId)
            .flatMap { response ->
                userStatusCache[userId] = response
                // Simple cache eviction after 1 minute
                Mono.delay(Duration.ofMinutes(1)).doOnNext { userStatusCache.remove(userId) }.subscribe()
                
                if (response.enabled == false) {
                    Mono.error<Void>(PolicyViolationException("User account is disabled"))
                } else {
                    Mono.empty<Void>()
                }
            }
            .onErrorResume { e ->
                if (e is PolicyViolationException) {
                    Mono.error(e)
                } else {
                    Mono.error(PolicyViolationException("User status check failed"))
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

    private fun extractChannel(session: WebSocketSession): String? {
        val query = session.handshakeInfo.uri.query ?: return null
        return query.split("&")
            .find { it.startsWith("channel=") }
            ?.substringAfter("channel=")
    }

    fun broadcastMessage(message: String) {
        userSinks.values.forEach { it.tryEmitNext(message) }
    }

    fun broadcastToChannel(channelId: String, message: String) {
        userSinks.forEach { (userId, sink) ->
            if (userChannels[userId] == channelId || channelId == "all") {
                sink.tryEmitNext(message)
            }
        }
    }

    fun sendMessageToUser(userId: String, channelId: String, message: String) {
        if (userChannels[userId] == channelId || channelId == "all") {
            userSinks[userId]?.tryEmitNext(message)
        }
    }

    private class PolicyViolationException(message: String) : RuntimeException(message)
}
