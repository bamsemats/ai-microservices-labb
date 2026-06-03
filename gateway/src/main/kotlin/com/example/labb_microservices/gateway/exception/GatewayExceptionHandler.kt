package com.example.labb_microservices.gateway.exception

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler
import org.springframework.core.annotation.Order
import org.springframework.core.io.buffer.DataBufferFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

@Component
@Order(-1)
class GatewayExceptionHandler(private val objectMapper: ObjectMapper) : ErrorWebExceptionHandler {
    private val logger = LoggerFactory.getLogger(GatewayExceptionHandler::class.java)

    override fun handle(exchange: ServerWebExchange, ex: Throwable): Mono<Void> {
        val response = exchange.response

        if (response.isCommitted) {
            return Mono.error(ex)
        }

        response.headers.contentType = MediaType.APPLICATION_JSON
        
        val status = when (ex) {
            is ResponseStatusException -> ex.statusCode
            else -> HttpStatus.INTERNAL_SERVER_ERROR
        }
        
        response.statusCode = status

        val message = when (ex) {
            is ResponseStatusException -> ex.reason ?: "An unexpected error occurred"
            else -> "An unexpected error occurred"
        }

        val errorBody = mapOf(
            "timestamp" to java.time.Instant.now().toString(),
            "path" to exchange.request.path.value(),
            "status" to status.value(),
            "error" to (status as? HttpStatus)?.reasonPhrase.orEmpty(),
            "message" to message
        )

        logger.error("Gateway Error [{}]: {} {}", exchange.request.path.value(), status.value(), ex.message)

        return try {
            val bytes = objectMapper.writeValueAsBytes(errorBody)
            val buffer = response.bufferFactory().wrap(bytes)
            response.writeWith(Mono.just(buffer))
        } catch (e: Exception) {
            logger.error("Failed to write error response", e)
            Mono.error(ex)
        }
    }
}
