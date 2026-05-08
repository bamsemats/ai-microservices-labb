package com.example.labb_microservices.common.security

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.*
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

@Component
class EncryptionUtils(
    @Value("\${encryption.secret}")
    private val secret: String
) {
    private val algorithm = "AES/GCM/NoPadding"
    private val keyAlgorithm = "AES"
    private val kdfAlgorithm = "PBKDF2WithHmacSHA256"
    private val hmacAlgorithm = "HmacSHA256"
    private val saltSize = 16
    private val nonceSize = 12
    private val tagSize = 128
    private val iterationCount = 65536
    private val keyLength = 256
    private val secureRandom = SecureRandom()

    init {
        if (secret.isBlank()) {
            throw IllegalStateException("Encryption secret cannot be blank")
        }
    }

    private fun deriveKey(salt: ByteArray): SecretKeySpec {
        val factory = SecretKeyFactory.getInstance(kdfAlgorithm)
        val spec = PBEKeySpec(secret.toCharArray(), salt, iterationCount, keyLength)
        val tmp = factory.generateSecret(spec)
        return SecretKeySpec(tmp.encoded, keyAlgorithm)
    }

    fun encrypt(data: String): String {
        val salt = ByteArray(saltSize)
        secureRandom.nextBytes(salt)
        val key = deriveKey(salt)

        val nonce = ByteArray(nonceSize)
        secureRandom.nextBytes(nonce)

        val cipher = Cipher.getInstance(algorithm)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(tagSize, nonce))
        
        val ciphertext = cipher.doFinal(data.toByteArray())
        
        val result = ByteArray(saltSize + nonceSize + ciphertext.size)
        System.arraycopy(salt, 0, result, 0, saltSize)
        System.arraycopy(nonce, 0, result, saltSize, nonceSize)
        System.arraycopy(ciphertext, 0, result, saltSize + nonceSize, ciphertext.size)
        
        return Base64.getEncoder().encodeToString(result)
    }

    fun decrypt(encryptedData: String): String {
        val data = Base64.getDecoder().decode(encryptedData)
        
        if (data.size < saltSize + nonceSize + (tagSize / 8)) {
            throw IllegalArgumentException("Encrypted data too short or corrupted")
        }

        val salt = ByteArray(saltSize)
        val nonce = ByteArray(nonceSize)
        val ciphertext = ByteArray(data.size - saltSize - nonceSize)
        
        System.arraycopy(data, 0, salt, 0, saltSize)
        System.arraycopy(data, saltSize, nonce, 0, nonceSize)
        System.arraycopy(data, saltSize + nonceSize, ciphertext, 0, ciphertext.size)
        
        val key = deriveKey(salt)
        val cipher = Cipher.getInstance(algorithm)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(tagSize, nonce))
        
        val decryptedBytes = cipher.doFinal(ciphertext)
        return String(decryptedBytes)
    }

    fun hash(data: String): String {
        val hmac = Mac.getInstance(hmacAlgorithm)
        val secretKey = SecretKeySpec(secret.toByteArray(), hmacAlgorithm)
        hmac.init(secretKey)
        val hashBytes = hmac.doFinal(data.toByteArray())
        return Base64.getEncoder().encodeToString(hashBytes)
    }

    fun encryptLegacy(data: String): String {
        val key = SecretKeySpec(secret.toByteArray().copyOf(32), "AES")
        val cipher = Cipher.getInstance("AES")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val encryptedBytes = cipher.doFinal(data.toByteArray())
        return Base64.getEncoder().encodeToString(encryptedBytes)
    }

    fun decryptLegacy(encryptedData: String): String {
        val key = SecretKeySpec(secret.toByteArray().copyOf(32), "AES")
        val cipher = Cipher.getInstance("AES")
        cipher.init(Cipher.DECRYPT_MODE, key)
        val decodedBytes = Base64.getDecoder().decode(encryptedData)
        val decryptedBytes = cipher.doFinal(decodedBytes)
        return String(decryptedBytes)
    }
}
