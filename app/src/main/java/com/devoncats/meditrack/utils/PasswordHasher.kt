package com.devoncats.meditrack.utils

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

object PasswordHasher {

    private const val SALT_BYTES = 16

    fun hash(rawPassword: String): String {
        val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
        val digest = digest(salt, rawPassword)
        return "${Base64.getEncoder().encodeToString(salt)}:${Base64.getEncoder().encodeToString(digest)}"
    }

    fun verify(rawPassword: String, storedHash: String): Boolean {
        val parts = storedHash.split(":")
        if (parts.size != 2) return false

        val salt = Base64.getDecoder().decode(parts[0])
        val expectedDigest = Base64.getDecoder().decode(parts[1])
        val actualDigest = digest(salt, rawPassword)

        return MessageDigest.isEqual(expectedDigest, actualDigest)
    }

    private fun digest(salt: ByteArray, rawPassword: String): ByteArray =
        MessageDigest.getInstance("SHA-256").apply {
            update(salt)
        }.digest(rawPassword.toByteArray(Charsets.UTF_8))
}
