package com.example.evolab.service.security

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class EncryptionServiceImpl(
    private val secretKeyBase64: String
) : EncryptionService {

    private val algorithm = "AES/GCM/NoPadding"
    private val key: SecretKey

    init {
        val decodedKey = Base64.getDecoder().decode(secretKeyBase64)
        if (decodedKey.size != 32) {
            throw IllegalArgumentException("A chave secreta deve ter 256 bits (32 bytes) para o AES-256")
        }
        key = SecretKeySpec(decodedKey, "AES")
    }

    override fun encrypt(plainText: String): String {
        val cipher = Cipher.getInstance(algorithm)
        val iv = ByteArray(12) // Tamanho padrão do IV para GCM é 12 bytes
        SecureRandom().nextBytes(iv)
        val parameterSpec = GCMParameterSpec(128, iv)

        cipher.init(Cipher.ENCRYPT_MODE, key, parameterSpec)
        val cipherText = cipher.doFinal(plainText.toByteArray())

        // Combina o IV com o texto cifrado para que possa ser decifrado mais tarde
        val ivAndCipherText = ByteArray(12 + cipherText.size)
        System.arraycopy(iv, 0, ivAndCipherText, 0, 12)
        System.arraycopy(cipherText, 0, ivAndCipherText, 12, cipherText.size)

        return Base64.getEncoder().encodeToString(ivAndCipherText)
    }

    override fun decrypt(encryptedText: String): String {
        val ivAndCipherText = Base64.getDecoder().decode(encryptedText)

        val iv = ByteArray(12)
        System.arraycopy(ivAndCipherText, 0, iv, 0, 12)

        val cipherText = ByteArray(ivAndCipherText.size - 12)
        System.arraycopy(ivAndCipherText, 12, cipherText, 0, cipherText.size)

        val cipher = Cipher.getInstance(algorithm)
        val parameterSpec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, parameterSpec)

        val plainText = cipher.doFinal(cipherText)
        return String(plainText)
    }
}
