package com.example.labb_microservices.message_service.controller

import com.example.labb_microservices.message_service.model.Frequency
import com.example.labb_microservices.message_service.service.FrequencyService
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

data class CreateFrequencyRequest(val name: String, val description: String? = null)

@RestController
@RequestMapping("/frequencies")
class FrequencyController(private val frequencyService: FrequencyService) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createFrequency(
        @RequestBody request: CreateFrequencyRequest,
        @AuthenticationPrincipal userId: String
    ): Mono<Frequency> {
        return frequencyService.createFrequency(request.name, request.description, userId)
    }

    @GetMapping
    fun getJoinedFrequencies(@AuthenticationPrincipal userId: String): Flux<Frequency> {
        return frequencyService.getJoinedFrequencies(userId)
    }

    @PostMapping("/{id}/join")
    fun joinFrequency(@PathVariable id: String, @AuthenticationPrincipal userId: String): Mono<Frequency> {
        return frequencyService.joinFrequency(id, userId)
    }

    @PostMapping("/{id}/leave")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun leaveFrequency(@PathVariable id: String, @AuthenticationPrincipal userId: String): Mono<Void> {
        return frequencyService.leaveFrequency(id, userId)
    }
}
