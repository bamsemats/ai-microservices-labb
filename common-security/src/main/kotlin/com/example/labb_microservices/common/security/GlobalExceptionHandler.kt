package com.example.labb_microservices.common.security

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.AuthenticationException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.bind.support.WebExchangeBindException
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.server.ServerWebExchange
import java.time.Instant

data class ErrorResponse(
    val timestamp: Instant = Instant.now(),
    val status: Int,
    val error: String,
    val message: String,
    val path: String
)

@RestControllerAdvice
class GlobalExceptionHandler {
    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(ResponseStatusException::class)
    fun handleResponseStatusException(
        ex: ResponseStatusException, 
        exchange: ServerWebExchange
    ): ResponseEntity<ErrorResponse> {
        val status = ex.statusCode
        val response = ErrorResponse(
            status = status.value(),
            error = HttpStatus.valueOf(status.value()).reasonPhrase,
            message = ex.reason ?: "No message available",
            path = exchange.request.path.value()
        )
        return ResponseEntity.status(status).body(response)
    }

    @ExceptionHandler(WebExchangeBindException::class)
    fun handleValidationException(
        ex: WebExchangeBindException, 
        exchange: ServerWebExchange
    ): ResponseEntity<ErrorResponse> {
        val errors = ex.bindingResult.fieldErrors
            .joinToString(", ") { "${it.field}: ${it.defaultMessage}" }
        
        val response = ErrorResponse(
            status = HttpStatus.BAD_REQUEST.value(),
            error = "Bad Request",
            message = "Validation failed: $errors",
            path = exchange.request.path.value()
        )
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response)
    }

    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDeniedException(
        ex: AccessDeniedException,
        exchange: ServerWebExchange
    ): ResponseEntity<ErrorResponse> {
        val response = ErrorResponse(
            status = HttpStatus.FORBIDDEN.value(),
            error = "Forbidden",
            message = ex.message ?: "Access Denied",
            path = exchange.request.path.value()
        )
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response)
    }

    @ExceptionHandler(AuthenticationException::class)
    fun handleAuthenticationException(
        ex: AuthenticationException,
        exchange: ServerWebExchange
    ): ResponseEntity<ErrorResponse> {
        val response = ErrorResponse(
            status = HttpStatus.UNAUTHORIZED.value(),
            error = "Unauthorized",
            message = ex.message ?: "Authentication Failed",
            path = exchange.request.path.value()
        )
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response)
    }

    @ExceptionHandler(Exception::class)
    fun handleGenericException(
        ex: Exception, 
        exchange: ServerWebExchange
    ): ResponseEntity<ErrorResponse> {
        logger.error("Unhandled exception at ${exchange.request.path.value()}", ex)
        
        val response = ErrorResponse(
            status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
            error = "Internal Server Error",
            message = ex.message ?: "An unexpected error occurred",
            path = exchange.request.path.value()
        )
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response)
    }
}
