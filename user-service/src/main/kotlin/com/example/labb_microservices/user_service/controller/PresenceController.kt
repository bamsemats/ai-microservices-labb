package com.example.labb_microservices.user_service.controller

import com.example.labb_microservices.user_service.exception.UserNotFoundException
import com.example.labb_microservices.user_service.model.PresenceStatus
import com.example.labb_microservices.user_service.model.PresenceUpdateEvent
import com.example.labb_microservices.user_service.service.PresenceService
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/users")
class PresenceController(private val presenceService: PresenceService) {

    @PutMapping("/status")
    fun updateStatus(
        @RequestBody status: PresenceStatus,
        @AuthenticationPrincipal principal: String
    ): Mono<Void> {
        return presenceService.updateStatus(principal, status)
            .onErrorMap { e ->
                if (e is UserNotFoundException) {
                    ResponseStatusException(HttpStatus.NOT_FOUND, e.message)
                } else {
                    e
                }
            }
    }

    @GetMapping("/presence")
    fun getAllPresences(): Flux<PresenceUpdateEvent> {
        return presenceService.getAllPresences()
    }

    @GetMapping("/{userId}/status")
    fun getStatus(@PathVariable userId: String): Mono<PresenceStatus> {
        return presenceService.getStatus(userId)
    }
}
