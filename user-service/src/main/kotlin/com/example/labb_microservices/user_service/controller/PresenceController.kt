package com.example.labb_microservices.user_service.controller

import com.example.labb_microservices.user_service.model.PresenceStatus
import com.example.labb_microservices.user_service.service.PresenceService
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono
import java.security.Principal

import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/users")
class PresenceController(private val presenceService: PresenceService) {

    @PutMapping("/status")
    fun updateStatus(
        @RequestBody status: PresenceStatus,
        @AuthenticationPrincipal principal: Principal
    ): Mono<Void> {
        return presenceService.updateStatus(principal.name, status)
            .onErrorMap { e ->
                if (e is RuntimeException && e.message == "User not found") {
                    ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")
                } else {
                    e
                }
            }
    }

    @GetMapping("/{userId}/status")
    fun getStatus(@PathVariable userId: String): Mono<PresenceStatus> {
        return presenceService.getStatus(userId)
    }
}
