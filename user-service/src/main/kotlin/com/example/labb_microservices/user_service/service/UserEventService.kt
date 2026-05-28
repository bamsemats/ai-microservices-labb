package com.example.labb_microservices.user_service.service

import com.example.labb_microservices.user_service.model.EventType
import com.example.labb_microservices.user_service.model.UserEvent
import com.example.labb_microservices.user_service.repository.UserEventRepository
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Service
class UserEventService(private val userEventRepository: UserEventRepository) {

    fun logEvent(userId: String, eventType: EventType, details: String? = null): Mono<UserEvent> {
        val event = UserEvent(userId = userId, eventType = eventType, details = details)
        return userEventRepository.save(event)
    }

    fun getUserEvents(userId: String): Flux<UserEvent> {
        return userEventRepository.findByUserIdOrderByTimestampDesc(userId)
    }

    fun getAllRecentEvents(): Flux<UserEvent> {
        return userEventRepository.findTop50ByOrderByTimestampDesc()
    }
}
