package com.example.labb_microservices.common.logging

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.pattern.CompositeConverter

class LogMaskingConverter : CompositeConverter<ILoggingEvent>() {
    
    private val maskPatterns = listOf(
        Regex("(?:password|secret|token|apiKey|api.key|authorization|bearer)\\s*[:=]\\s*([^\\s,]+)", RegexOption.IGNORE_CASE),
        Regex("(?:Bearer\\s+)([a-zA-Z0-9._-]+)", RegexOption.IGNORE_CASE)
    )

    override fun transform(event: ILoggingEvent, inStr: String): String {
        var message = inStr
        
        maskPatterns.forEach { pattern ->
            message = message.replace(pattern) { match ->
                val fullMatch = match.value
                val sensitivePart = match.groups[1]?.value
                if (sensitivePart != null && sensitivePart.length > 4) {
                    fullMatch.replace(sensitivePart, "********")
                } else {
                    fullMatch
                }
            }
        }
        
        return message
    }
}
