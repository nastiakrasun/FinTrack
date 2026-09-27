package com.fintrack.app.domain.repository

enum class AuthResult {
    SUCCESS,
    EMAIL_ALREADY_REGISTERED,
    INVALID_CREDENTIALS
}

interface AuthRepository {
    fun register(email: String, password: CharArray): AuthResult

    fun login(email: String, password: CharArray): AuthResult
}
