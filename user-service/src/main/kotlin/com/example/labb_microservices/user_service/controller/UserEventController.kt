package com.example.labb_microservices.user_service.controller

import com.example.labb_microservices.user_service.model.EventType
import com.example.labb_microservices.user_service.model.UserEvent
import com.example.labb_microservices.user_service.service.UserEventService
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

data class LogEventRequest(val eventType: EventType, val details: String? = null)

@RestController
@RequestMapping("/events")
class UserEventController(private val userEventService: UserEventService) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun logEvent(
        @AuthenticationPrincipal userId: String,
        @RequestBody request: LogEventRequest
    ): Mono<UserEvent> {
        return userEventService.logEvent(userId, request.eventType, request.details)
    }

    @GetMapping
    fun getMyEvents(@AuthenticationPrincipal userId: String): Flux<UserEvent> {
        return userEventService.getUserEvents(userId)
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    fun getAllRecentEvents(): Flux<UserEvent> {
        return userEventService.getAllRecentEvents()
    }
}
