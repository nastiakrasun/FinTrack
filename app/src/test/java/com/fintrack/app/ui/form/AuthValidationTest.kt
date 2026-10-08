package com.fintrack.app.ui.form

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class AuthValidationTest {

    @Test
    fun emailIsRequired() {
        assertNotNull(validateEmail(""))
        assertNotNull(validateEmail("   "))
    }

    @Test
    fun emailMustHaveValidFormat() {
        listOf("plainaddress", "user@", "@example.com", "user@example", "user @example.com", "a@b..com")
            .forEach { assertNotNull("Expected error for '$it'", validateEmail(it)) }
        listOf("user@example.com", " user.name+tag@mail.example.org ", "a-b@sub-domain.no")
            .forEach { assertNull("Expected no error for '$it'", validateEmail(it)) }
    }

    @Test
    fun passwordIsRequiredAndNeedsMinimumLength() {
        assertEquals("Enter your password.", validatePassword(""))
        assertNotNull(validatePassword("1234567"))
        assertNull(validatePassword("12345678"))
    }

    @Test
    fun confirmPasswordIsRequiredAndMustMatch() {
        assertEquals("Confirm your password.", validateConfirmPassword("password1", ""))
        assertNotNull(validateConfirmPassword("password1", "password2"))
        assertNull(validateConfirmPassword("password1", "password1"))
    }
}
