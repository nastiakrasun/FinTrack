package com.fintrack.app.data.repository

import com.fintrack.app.domain.repository.AuthRepository
import com.fintrack.app.domain.repository.AuthResult
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Locale
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class InMemoryDemoAuthRepository : AuthRepository {
    private val users = mutableMapOf<String, Credentials>()
    private val secureRandom = SecureRandom()

    override fun register(email: String, password: CharArray): AuthResult {
        val normalizedEmail = normalizeEmail(email)
        if (normalizedEmail in users) {
            password.fill('\u0000')
            return AuthResult.EMAIL_ALREADY_REGISTERED
        }

        val salt = ByteArray(SALT_LENGTH_BYTES).also(secureRandom::nextBytes)
        val passwordHash = hashPassword(password, salt)
        users[normalizedEmail] = Credentials(salt, passwordHash)
        return AuthResult.SUCCESS
    }

    override fun login(email: String, password: CharArray): AuthResult {
        val credentials = users[normalizeEmail(email)]
        if (credentials == null) {
            password.fill('\u0000')
            return AuthResult.INVALID_CREDENTIALS
        }

        val attemptedHash = hashPassword(password, credentials.salt)
        return if (MessageDigest.isEqual(credentials.passwordHash, attemptedHash)) {
            AuthResult.SUCCESS
        } else {
            AuthResult.INVALID_CREDENTIALS
        }
    }

    private fun hashPassword(password: CharArray, salt: ByteArray): ByteArray {
        val keySpec = PBEKeySpec(password, salt, ITERATIONS, HASH_LENGTH_BITS)
        password.fill('\u0000')
        return try {
            SecretKeyFactory.getInstance(KEY_ALGORITHM)
                .generateSecret(keySpec)
                .encoded
        } finally {
            keySpec.clearPassword()
        }
    }

    private fun normalizeEmail(email: String): String =
        email.trim().lowercase(Locale.ROOT)

    private data class Credentials(
        val salt: ByteArray,
        val passwordHash: ByteArray
    )

    private companion object {
        const val KEY_ALGORITHM = "PBKDF2WithHmacSHA256"
        const val ITERATIONS = 120_000
        const val HASH_LENGTH_BITS = 256
        const val SALT_LENGTH_BYTES = 16
    }
}
