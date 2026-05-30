package com.example.labb_microservices.message_service.controller

import com.example.labb_microservices.message_service.model.Frequency
import com.example.labb_microservices.message_service.service.FrequencyService
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

data class CreateFrequencyRequest(
    @field:NotBlank(message = "Frequency name cannot be blank")
    @field:Size(min = 2, max = 50, message = "Name must be between 2 and 50 characters")
    val name: String, 
    
    val description: String? = null
)

data class RenameFrequencyRequest(
    @field:NotBlank(message = "New name cannot be blank")
    @field:Size(min = 2, max = 50, message = "Name must be between 2 and 50 characters")
    val name: String
)

@RestController
@RequestMapping("/frequencies")
class FrequencyController(private val frequencyService: FrequencyService) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createFrequency(
        @Valid @RequestBody request: CreateFrequencyRequest,
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

    @PutMapping("/{id}/rename")
    fun renameFrequency(
        @PathVariable id: String, 
        @Valid @RequestBody request: RenameFrequencyRequest, 
        @AuthenticationPrincipal userId: String
    ): Mono<Frequency> {
        return frequencyService.renameFrequency(id, request.name, userId)
    }

    @PostMapping("/{id}/members/{memberId}")
    fun inviteMember(
        @PathVariable id: String, 
        @PathVariable memberId: String, 
        @AuthenticationPrincipal userId: String
    ): Mono<Frequency> {
        return frequencyService.inviteMember(id, memberId, userId)
    }

    @DeleteMapping("/{id}/members/{memberId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun kickMember(
        @PathVariable id: String, 
        @PathVariable memberId: String, 
        @AuthenticationPrincipal userId: String
    ): Mono<Void> {
        return frequencyService.kickMember(id, memberId, userId)
    }
}
