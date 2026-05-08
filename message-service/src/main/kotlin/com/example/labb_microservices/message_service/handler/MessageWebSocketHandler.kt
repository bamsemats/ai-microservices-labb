package com.example.labb_microservices.message_service.handler

import com.example.labb_microservices.common.security.JwtTokenValidator
import com.example.labb_microservices.message_service.client.UserGrpcClient
import com.example.labb_microservices.message_service.service.PresenceService
import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.reactive.socket.CloseStatus
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.Sinks
import java.time.Duration
import java.util.concurrent.TimeUnit
import org.springframework.beans.factory.annotation.Value

@Component
class MessageWebSocketHandler(
    private val jwtTokenValidator: JwtTokenValidator,
    private val userGrpcClient: UserGrpcClient,
    private val presenceService: PresenceService,
    private val objectMapper: com.fasterxml.jackson.databind.ObjectMapper,
    private val sessionRegistry: com.example.labb_microservices.message_service.session.SessionRegistry
) : WebSocketHandler {

    private val logger = LoggerFactory.getLogger(MessageWebSocketHandler::class.java)

    internal val policyViolations = java.util.concurrent.atomic.AtomicInteger(0)

    @Value("\${auth.cache.ttl:60}")
    private var cacheTtlSeconds: Long = 60

    @Value("\${auth.validation.interval:10}")
    private var validationIntervalSeconds: Long = 10

    private val userStatusCache: Cache<String, com.example.labb_microservices.proto.UserResponse> by lazy {
        Caffeine.newBuilder()
            .expireAfterWrite(cacheTtlSeconds, TimeUnit.SECONDS)
            .maximumSize(1000)
            .build()
    }

    override fun handle(session: WebSocketSession): Mono<Void> {
        val initialToken = extractToken(session)
        val channelId = extractChannel(session) ?: "general"
        val sessionId = session.id

        val authenticatedUserId = Sinks.one<String>()
        val disconnectSink = Sinks.empty<Void>()
        val sink = Sinks.many().multicast().directBestEffort<String>()

        val chatSession = com.example.labb_microservices.message_service.session.ChatSession(
            sessionId = sessionId,
            channelId = channelId,
            sink = sink,
            disconnectSink = disconnectSink
        )

        sessionRegistry.register(chatSession)

        if (initialToken != null && jwtTokenValidator.validateToken(initialToken)) {
            val userId = jwtTokenValidator.getUserIdFromToken(initialToken)
            if (userId != null) {
                sessionRegistry.promoteSession(sessionId, userId, initialToken)
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
                                sessionRegistry.promoteSession(sessionId, userId, token)
                                authenticatedUserId.tryEmitValue(userId)
                            }
                        }
                    }
                } catch (e: Exception) {
                    logger.debug("Failed to parse incoming WebSocket frame from session {}: {}", sessionId, e.message)
                }
            }
            .doFinally {
                val unregisteredSession = sessionRegistry.unregister(sessionId)
                val userId = unregisteredSession?.userId
                logger.info("WebSocket session ending for user: {}, session: {}", userId ?: "anonymous", sessionId)
                disconnectSink.tryEmitEmpty()
                
                if (userId != null && !sessionRegistry.isUserOnline(userId)) {
                    presenceService.setUserOffline(userId)
                        .doOnError { e -> logger.error("Failed to set user offline: $userId", e) }
                        .onErrorResume { Mono.empty() }
                        .subscribe()
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
                val validation = Flux.interval(Duration.ofSeconds(validationIntervalSeconds))
                    .flatMap {
                        val currentSession = sessionRegistry.getSession(sessionId)
                        val token = currentSession?.token
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

        val securityTask = authenticationTasks
            .onErrorResume { e ->
                if (e is PolicyViolationException) {
                    policyViolations.incrementAndGet()
                    logger.warn("Closing session due to policy violation: {}", e.message)
                    session.close(CloseStatus(1008, e.message))
                        .delayElement(Duration.ofMillis(500))
                        .then()
                } else {
                    logger.error("WebSocket error for session $sessionId", e)
                    session.close(CloseStatus.SERVER_ERROR)
                        .delayElement(Duration.ofMillis(500))
                        .then()
                }
            }

        return Mono.`when`(input, output, securityTask)
    }

    private fun checkUserStatus(userId: String): Mono<Void> {
        // ... (unchanged)
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

    private class PolicyViolationException(message: String) : RuntimeException(message)
}
