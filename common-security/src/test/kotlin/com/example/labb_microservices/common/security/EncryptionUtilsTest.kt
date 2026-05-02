package com.example.labb_microservices.common.security

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

class EncryptionUtilsTest {

    private val encryptionUtils = EncryptionUtils("a-very-long-and-secure-encryption-secret-key-32-chars")

    @Test
    fun `should encrypt and decrypt data`() {
        val originalData = "test@example.com"
        val encryptedData = encryptionUtils.encrypt(originalData)
        val decryptedData = encryptionUtils.decrypt(encryptedData)

        assertNotEquals(originalData, encryptedData)
        assertEquals(originalData, decryptedData)
    }

    @Test
    fun `should generate consistent hashes for same data`() {
        val data = "test@example.com"
        val hash1 = encryptionUtils.hash(data)
        val hash2 = encryptionUtils.hash(data)

        assertEquals(hash1, hash2)
    }

    @Test
    fun `should generate different hashes for different data`() {
        val data1 = "test1@example.com"
        val data2 = "test2@example.com"
        val hash1 = encryptionUtils.hash(data1)
        val hash2 = encryptionUtils.hash(data2)

        assertNotEquals(hash1, hash2)
    }
}
