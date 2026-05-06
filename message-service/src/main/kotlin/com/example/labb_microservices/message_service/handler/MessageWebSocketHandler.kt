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
import java.util.concurrent.CopyOnWriteArraySet

@Component
class MessageWebSocketHandler(
    private val jwtTokenValidator: JwtTokenValidator,
    private val userGrpcClient: UserGrpcClient,
    private val presenceService: PresenceService
) : WebSocketHandler {

    private val logger = LoggerFactory.getLogger(MessageWebSocketHandler::class.java)

    @org.springframework.beans.factory.annotation.Value("\${auth.cache.ttl:60}")
    private var cacheTtlSeconds: Long = 60

    private val objectMapper = com.fasterxml.jackson.databind.ObjectMapper()
    private val sessionSinks = ConcurrentHashMap<String, Sinks.Many<String>>()
    private val sessionChannels = ConcurrentHashMap<String, String>()
    private val sessionTokens = ConcurrentHashMap<String, String>()
    private val sessionUsers = ConcurrentHashMap<String, String>()
    private val userSessions = ConcurrentHashMap<String, MutableSet<String>>()
    private val userStatusCache = ConcurrentHashMap<String, com.example.labb_microservices.proto.UserResponse>()

    override fun handle(session: WebSocketSession): Mono<Void> {
        val initialToken = extractToken(session)
        val channelId = extractChannel(session) ?: "general"
        val sessionId = session.id

        val authenticatedUserId = Sinks.one<String>()
        val disconnectSink = Sinks.empty<Void>()
        val sink = Sinks.many().multicast().directBestEffort<String>()

        sessionSinks[sessionId] = sink
        sessionChannels[sessionId] = channelId

        if (initialToken != null && jwtTokenValidator.validateToken(initialToken)) {
            val userId = jwtTokenValidator.getUserIdFromToken(initialToken)
            if (userId != null) {
                sessionTokens[sessionId] = initialToken
                registerSession(sessionId, userId, channelId)
                authenticatedUserId.tryEmitValue(userId)
            }
        }

        val input = session.receive()
            .doOnNext { message ->
                val payload = message.payloadAsText
                try {
                    val json = objectMapper.readTree(payload)
                    if (json.has("type") && json.get("type").asText() == "auth" && json.has("token")) {
                        val token = json.get("token").asText()
                        if (jwtTokenValidator.validateToken(token)) {
                            val userId = jwtTokenValidator.getUserIdFromToken(token)
                            if (userId != null) {
                                logger.info("User {} authenticated via message in session {}", userId, sessionId)
                                sessionTokens[sessionId] = token
                                registerSession(sessionId, userId, channelId)
                                authenticatedUserId.tryEmitValue(userId)
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Ignore non-json or malformed for now
                }
            }
            .doFinally {
                val userId = sessionUsers[sessionId]
                logger.info("WebSocket session ending for user: {}, session: {}", userId ?: "anonymous", sessionId)
                disconnectSink.tryEmitEmpty()
                sessionSinks.remove(sessionId)
                sessionChannels.remove(sessionId)
                sessionTokens.remove(sessionId)
                sessionUsers.remove(sessionId)
                if (userId != null) {
                    val sessions = userSessions[userId]
                    sessions?.remove(sessionId)
                    if (sessions?.isEmpty() == true) {
                        userSessions.remove(userId)
                        presenceService.setUserOffline(userId)
                            .doOnError { e -> logger.error("Failed to set user offline: $userId", e) }
                            .onErrorResume { Mono.empty() }
                            .subscribe()
                    }
                }
            }
            .then()

        val output = session.send(
            sink.asFlux()
                .takeUntilOther(disconnectSink.asMono())
                .map { session.textMessage(it) }
        )

        val authenticationTasks = authenticatedUserId.asMono()
            .timeout(Duration.ofSeconds(5))
            .onErrorMap(java.util.concurrent.TimeoutException::class.java) { 
                PolicyViolationException("Authentication timeout - please send auth token") 
            }
            .flatMap { userId ->
                val validation = Flux.interval(Duration.ofSeconds(10))
                    .flatMap {
                        val token = sessionTokens[sessionId]
                        if (token != null && !jwtTokenValidator.validateToken(token)) {
                            Mono.error<Void>(PolicyViolationException("Token invalid"))
                        } else {
                            checkUserStatus(userId)
                        }
                    }
                    .takeUntilOther(disconnectSink.asMono())
                    .then()

                val presenceHeartbeat = presenceService.setUserOnline(userId)
                    .onErrorResume { Mono.empty() }
                    .thenMany(Flux.interval(Duration.ofMinutes(1)))
                    .flatMap { presenceService.setUserOnline(userId).onErrorResume { Mono.empty() } }
                    .takeUntilOther(disconnectSink.asMono())
                    .then()

                Mono.`when`(validation, presenceHeartbeat)
            }

        return Mono.`when`(input, output, authenticationTasks)
            .onErrorResume { e ->
                if (e is PolicyViolationException) {
                    logger.warn("Closing session due to policy violation: {}", e.message)
                    session.close(CloseStatus(1008, e.message))
                } else {
                    logger.error("WebSocket error for session $sessionId", e)
                    session.close(CloseStatus.SERVER_ERROR)
                }
            }
    }

    private fun registerSession(sessionId: String, userId: String, channelId: String) {
        val oldUserId = sessionUsers[sessionId]
        if (oldUserId != null && oldUserId != userId) {
            val oldSessions = userSessions[oldUserId]
            oldSessions?.remove(sessionId)
            if (oldSessions?.isEmpty() == true) {
                userSessions.remove(oldUserId)
            }
        }
        sessionUsers[sessionId] = userId
        userSessions.computeIfAbsent(userId) { CopyOnWriteArraySet() }.add(sessionId)
    }

    private fun checkUserStatus(userId: String): Mono<Void> {
        if (cacheTtlSeconds > 0) {
            val cached = userStatusCache.getIfPresent(userId)
            if (cached != null) {
                return if (cached.enabled == false) {
                    Mono.error(PolicyViolationException("User account is disabled"))
                } else {
                    Mono.empty()
                }
            }
        }

        return userGrpcClient.getUser(userId)
            .flatMap { response ->
                if (cacheTtlSeconds > 0) {
                    userStatusCache.put(userId, response)
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
                    logger.warn("Transient failure checking user status for {}, bypassing check: {}", userId, e.message)
                    Mono.empty()
                }
            }
    }

    private fun extractToken(session: WebSocketSession): String? {
        val authHeader = session.handshakeInfo.headers.getFirst("Authorization")
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7)
        }
        return null
    }

    private fun extractChannel(session: WebSocketSession): String? {
        val query = session.handshakeInfo.uri.query ?: return null
        return query.split("&")
            .find { it.startsWith("channel=") }
            ?.substringAfter("channel=")
    }

    fun broadcastMessage(message: String) {
        sessionSinks.values.forEach { it.tryEmitNext(message) }
    }

    fun broadcastToChannel(channelId: String, message: String) {
        sessionSinks.forEach { (sessionId, sink) ->
            if (sessionChannels[sessionId] == channelId || channelId == "all") {
                sink.tryEmitNext(message)
            }
        }
    }

    fun sendMessageToUser(userId: String, channelId: String, message: String) {
        userSessions[userId]?.forEach { sessionId ->
            if (sessionChannels[sessionId] == channelId || channelId == "all") {
                sessionSinks[sessionId]?.tryEmitNext(message)
            }
        }
    }

    private class PolicyViolationException(message: String) : RuntimeException(message)
}
sionId] == channelId || channelId == "all") {
                sink.tryEmitNext(message)
            }
        }
    }

    fun sendMessageToUser(userId: String, channelId: String, message: String) {
        userSessions[userId]?.forEach { sessionId ->
            if (sessionChannels[sessionId] == channelId || channelId == "all") {
                sessionSinks[sessionId]?.tryEmitNext(message)
            }
        }
    }

    private class PolicyViolationException(message: String) : RuntimeException(message)
}
