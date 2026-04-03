package com.example.evolab.service.security

interface EncryptionService {
    fun encrypt(plainText: String): String
    fun decrypt(encryptedText: String): String
}
