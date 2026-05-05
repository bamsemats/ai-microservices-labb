package com.example.labb_microservices.message_service.handler

import com.example.labb_microservices.common.security.JwtTokenValidator
import com.example.labb_microservices.message_service.client.UserGrpcClient
import com.example.labb_microservices.message_service.service.PresenceService
import org.slf4j.LoggerFactory
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
    private val userGrpcClient: UserGrpcClient,
    private val presenceService: PresenceService
) : WebSocketHandler {

    private val logger = LoggerFactory.getLogger(MessageWebSocketHandler::class.java)

    @org.springframework.beans.factory.annotation.Value("\${auth.cache.ttl:60}")
    private var cacheTtlSeconds: Long = 60

    private val userSinks = ConcurrentHashMap<String, Sinks.Many<String>>()
    private val userChannels = ConcurrentHashMap<String, String>()
    private val userStatusCache = ConcurrentHashMap<String, com.example.labb_microservices.proto.UserResponse>()

    override fun handle(session: WebSocketSession): Mono<Void> {
        val token = extractToken(session)
        val channelId = extractChannel(session) ?: "general"

        val principalMono = if (token != null) {
            if (jwtTokenValidator.validateToken(token)) {
                val userId = jwtTokenValidator.getUserIdFromToken(token)
                if (userId != null) Mono.just(userId) else Mono.empty<String>()
            } else {
                Mono.empty<String>()
            }
        } else {
            session.handshakeInfo.principal.map { it.name }
        }

        return principalMono
            .flatMap { userId: String ->
                logger.info("New WebSocket connection for user: {} in channel: {}", userId, channelId)
                
                val sink = userSinks.computeIfAbsent(userId) {
                    Sinks.many().multicast().directBestEffort()
                }
                userChannels[userId] = channelId

                // Use a single completion sink to coordinate termination
                val disconnectSink = Sinks.empty<Void>()

                // Input stream - triggers disconnectSink on completion
                val input = session.receive()
                    .doOnTerminate {
                        logger.info("WebSocket input stream terminated for user: {}", userId)
                        disconnectSink.tryEmitEmpty()
                        userSinks.remove(userId, sink)
                        userChannels.remove(userId)
                        presenceService.setUserOffline(userId).subscribe()
                    }
                    .then()

                // Output stream - stop when disconnectSink completes
                val output = session.send(
                    sink.asFlux()
                        .takeUntilOther(disconnectSink.asMono())
                        .map { session.textMessage(it) }
                )

                // Periodic validation - stop when disconnectSink completes
                val validation = Flux.interval(Duration.ofSeconds(10))
                    .flatMap {
                        if (token != null && !jwtTokenValidator.validateToken(token)) {
                            Mono.error<Void>(PolicyViolationException("Token invalid"))
                        } else {
                            checkUserStatus(userId)
                        }
                    }
                    .takeUntilOther(disconnectSink.asMono())
                    .then()

                // Global Presence Heartbeat - stop when disconnectSink completes
                val presenceHeartbeat = presenceService.setUserOnline(userId)
                    .thenMany(Flux.interval(Duration.ofMinutes(1)))
                    .flatMap { presenceService.setUserOnline(userId) }
                    .takeUntilOther(disconnectSink.asMono())
                    .then()

                // Execute all tasks concurrently
                Mono.`when`(input, output, validation, presenceHeartbeat)
                    .onErrorResume { e ->
                        if (e is PolicyViolationException) {
                            logger.warn("Closing session due to policy violation: {}", e.message)
                            session.close(CloseStatus(1008, e.message))
                        } else {
                            logger.error("WebSocket error for user $userId", e)
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
            return if (cached.enabled == false) {
                Mono.error(PolicyViolationException("User account is disabled"))
            } else {
                Mono.empty()
            }
        }

        return userGrpcClient.getUser(userId)
            .flatMap { response ->
                userStatusCache[userId] = response
                if (cacheTtlSeconds > 0) {
                    Mono.delay(Duration.ofSeconds(cacheTtlSeconds))
                        .publishOn(reactor.core.scheduler.Schedulers.boundedElastic())
                        .doOnNext { userStatusCache.remove(userId) }
                        .doOnError { e -> logger.error("Cache eviction failed", e) }
                        .subscribe()
                }
                
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
