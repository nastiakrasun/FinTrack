package com.fintrack.app.ui.form

const val MIN_PASSWORD_LENGTH = 8

private val EMAIL_PATTERN = Regex(
    "^[A-Za-z0-9._%+\\-]+@([A-Za-z0-9]([A-Za-z0-9\\-]*[A-Za-z0-9])?\\.)+[A-Za-z]{2,}$"
)

fun validateEmail(email: String): String? {
    val value = email.trim()
    return when {
        value.isEmpty() -> "Enter your email address."
        !EMAIL_PATTERN.matches(value) -> "Enter a valid email, for example name@example.com."
        else -> null
    }
}

fun validatePassword(password: String): String? = when {
    password.isEmpty() -> "Enter your password."
    password.length < MIN_PASSWORD_LENGTH ->
        "Password must be at least $MIN_PASSWORD_LENGTH characters (now ${password.length})."

    else -> null
}

fun validateConfirmPassword(password: String, confirmPassword: String): String? = when {
    confirmPassword.isEmpty() -> "Confirm your password."
    password != confirmPassword -> "Passwords do not match. Re-enter the same password."
    else -> null
}
