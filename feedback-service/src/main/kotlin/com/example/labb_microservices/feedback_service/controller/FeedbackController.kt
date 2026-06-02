package com.example.labb_microservices.feedback_service.controller

import com.example.labb_microservices.feedback_service.model.Feedback
import com.example.labb_microservices.feedback_service.repository.FeedbackRepository
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.slf4j.LoggerFactory
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono
import java.time.Instant

data class FeedbackRequest(
    @field:Min(1) @field:Max(5) val rating: Int,
    @field:NotBlank @field:Size(max = 2000) val comment: String
)

@RestController
@RequestMapping("/feedback")
class FeedbackController(private val feedbackRepository: FeedbackRepository) {

    private val logger = LoggerFactory.getLogger(FeedbackController::class.java)

    @PostMapping
    fun submitFeedback(@Valid @RequestBody request: FeedbackRequest): Mono<Feedback> {
        return ReactiveSecurityContextHolder.getContext()
            .map { it.authentication.name }
            .defaultIfEmpty("anonymous")
            .flatMap { userId ->
                val feedback = Feedback(
                    userId = if (userId == "anonymous") null else userId,
                    rating = request.rating,
                    comment = request.comment,
                    timestamp = Instant.now()
                )
                logger.info("Saving user feedback for user=<redacted>")
                feedbackRepository.save(feedback)
            }
    }

    @GetMapping
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    fun getAllFeedback(): reactor.core.publisher.Flux<Feedback> {
        return feedbackRepository.findAll()
    }
}
