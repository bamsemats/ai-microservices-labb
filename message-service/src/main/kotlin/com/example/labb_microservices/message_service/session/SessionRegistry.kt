package com.example.labb_microservices.message_service.session

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet

@Service
class SessionRegistry {
    private val logger = LoggerFactory.getLogger(SessionRegistry::class.java)

    private val sessions = ConcurrentHashMap<String, ChatSession>()
    private val userSessions = ConcurrentHashMap<String, MutableSet<String>>()

    fun register(session: ChatSession) {
        sessions[session.sessionId] = session
        session.userId?.let { userId ->
            userSessions.computeIfAbsent(userId) { CopyOnWriteArraySet() }.add(session.sessionId)
        }
        logger.debug("Registered session {} for user {}", session.sessionId, session.userId ?: "anonymous")
    }

    fun promoteSession(sessionId: String, userId: String, token: String, username: String? = null) {
        val session = sessions[sessionId] ?: return
        
        // If it was already registered to another user (unlikely but possible), clean up
        session.userId?.let { oldUserId ->
            if (oldUserId != userId) {
                userSessions.computeIfPresent(oldUserId) { _, sessions ->
                    sessions.remove(sessionId)
                    if (sessions.isEmpty()) null else sessions
                }
            }
        }

        session.userId = userId
        session.username = username
        session.token = token
        userSessions.computeIfAbsent(userId) { CopyOnWriteArraySet() }.add(sessionId)
        logger.debug("Promoted session {} to user {} ({})", sessionId, userId, username ?: "no-name")
    }

    fun unregister(sessionId: String): ChatSession? {
        val session = sessions.remove(sessionId)
        if (session != null) {
            session.userId?.let { userId ->
                userSessions.computeIfPresent(userId) { _, sessions ->
                    sessions.remove(sessionId)
                    if (sessions.isEmpty()) null else sessions
                }
            }
            logger.debug("Unregistered session {} for user {}", sessionId, session.userId ?: "anonymous")
        }
        return session
    }

    fun getSession(sessionId: String): ChatSession? = sessions[sessionId]

    fun getSessionsForUser(userId: String): List<ChatSession> {
        return userSessions[userId]?.mapNotNull { sessions[it] } ?: emptyList()
    }

    fun getSessionsForChannel(channelId: String): List<ChatSession> {
        return sessions.values.filter { it.channelId == channelId }
    }

    fun getAllSessions(): Collection<ChatSession> = sessions.values

    fun isUserOnline(userId: String): Boolean {
        return userSessions.containsKey(userId) && userSessions[userId]?.isNotEmpty() == true
    }
}
