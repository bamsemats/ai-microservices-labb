package com.example.labb_microservices.ai_service.logic

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class PiiRedactor {
    private val logger = LoggerFactory.getLogger(PiiRedactor::class.java)

    // Basic regex for email
    private val emailRegex = Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")
    // More aggressive phone regex that handles spaces, dashes, and international prefixes
    private val phoneRegex = Regex("(\\+?\\d[\\d-.\\s]{7,20}\\d)")

    fun redact(content: String): String {
        var redacted = content
        
        // Redact Emails
        if (emailRegex.containsMatchIn(redacted)) {
            logger.debug("Redacting email pattern from prompt")
            redacted = redacted.replace(emailRegex, "[EMAIL_REDACTED]")
        }
        
        // Redact Phones
        if (phoneRegex.containsMatchIn(redacted)) {
            logger.debug("Redacting phone pattern from prompt")
            redacted = redacted.replace(phoneRegex, "[PHONE_REDACTED]")
        }

        return redacted
    }
}
