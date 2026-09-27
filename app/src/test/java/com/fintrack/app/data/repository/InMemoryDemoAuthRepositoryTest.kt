package com.fintrack.app.data.repository

import com.fintrack.app.domain.repository.AuthResult
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class InMemoryDemoAuthRepositoryTest {
    private val repository = InMemoryDemoAuthRepository()

    @Test
    fun registeredUserCanLogInWithNormalizedEmail() {
        assertEquals(
            AuthResult.SUCCESS,
            repository.register("User@example.com", "secure-pass".toCharArray())
        )
        assertEquals(
            AuthResult.SUCCESS,
            repository.login(" user@EXAMPLE.com ", "secure-pass".toCharArray())
        )
    }

    @Test
    fun rejectsDuplicateEmailAndIncorrectPassword() {
        repository.register("user@example.com", "secure-pass".toCharArray())

        assertEquals(
            AuthResult.EMAIL_ALREADY_REGISTERED,
            repository.register(" USER@example.com ", "different-pass".toCharArray())
        )
        assertEquals(
            AuthResult.INVALID_CREDENTIALS,
            repository.login("user@example.com", "wrong-pass".toCharArray())
        )
    }

    @Test
    fun clearsPasswordCharacterArrayAfterRegistration() {
        val password = "secure-pass".toCharArray()

        repository.register("user@example.com", password)

        assertArrayEquals(CharArray(password.size), password)
    }
}
