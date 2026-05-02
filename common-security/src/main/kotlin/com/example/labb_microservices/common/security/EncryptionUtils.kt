package com.example.labb_microservices.common.security

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.security.MessageDigest
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

@Component
class EncryptionUtils(
    @Value("\${encryption.secret:a-very-long-and-secure-encryption-secret-key-32-chars}")
    private val secret: String
) {
    private val algorithm = "AES"
    private val hashingAlgorithm = "SHA-256"

    fun encrypt(data: String): String {
        val key = SecretKeySpec(secret.toByteArray().copyOf(32), algorithm)
        val cipher = Cipher.getInstance(algorithm)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val encryptedBytes = cipher.doFinal(data.toByteArray())
        return Base64.getEncoder().encodeToString(encryptedBytes)
    }

    fun decrypt(encryptedData: String): String {
        val key = SecretKeySpec(secret.toByteArray().copyOf(32), algorithm)
        val cipher = Cipher.getInstance(algorithm)
        cipher.init(Cipher.DECRYPT_MODE, key)
        val decodedBytes = Base64.getDecoder().decode(encryptedData)
        val decryptedBytes = cipher.doFinal(decodedBytes)
        return String(decryptedBytes)
    }

    fun hash(data: String): String {
        val digest = MessageDigest.getInstance(hashingAlgorithm)
        val hashBytes = digest.digest(data.toByteArray())
        return Base64.getEncoder().encodeToString(hashBytes)
    }
}
