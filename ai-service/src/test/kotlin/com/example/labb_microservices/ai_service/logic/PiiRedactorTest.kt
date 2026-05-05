package com.example.labb_microservices.ai_service.logic

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PiiRedactorTest {

    private val redactor = PiiRedactor()

    @Test
    fun `should redact email addresses`() {
        val content = "Contact me at mats@example.com for more info."
        val result = redactor.redact(content)
        assertEquals("Contact me at [EMAIL_REDACTED] for more info.", result)
    }

    @Test
    fun `should redact multiple emails`() {
        val content = "mats@example.com and john.doe123@sub.domain.co.uk are here."
        val result = redactor.redact(content)
        assertEquals("[EMAIL_REDACTED] and [EMAIL_REDACTED] are here.", result)
    }

    @Test
    fun `should redact phone numbers`() {
        val content = "Call me at +46 70 123 45 67 tomorrow."
        val result = redactor.redact(content)
        assertEquals("Call me at [PHONE_REDACTED] tomorrow.", result)
    }

    @Test
    fun `should not redact small numbers that are not phones`() {
        val content = "The count is 12345."
        val result = redactor.redact(content)
        assertEquals("The count is 12345.", result)
    }

    @Test
    fun `should redact both email and phone in same content`() {
        val content = "Email mats@example.com or call 070-1234567"
        val result = redactor.redact(content)
        assertEquals("Email [EMAIL_REDACTED] or call [PHONE_REDACTED]", result)
    }

    @Test
    fun `should not redact dates or long IDs`() {
        val content1 = "Event on 2026-05-01"
        assertEquals(content1, redactor.redact(content1))

        val content2 = "Order ID 1234567890123"
        assertEquals(content2, redactor.redact(content2))
    }
}
