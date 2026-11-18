package com.fintrack.app.domain.repository

sealed interface DemoApiResult {
    data class Success(val authenticated: Boolean, val token: String?) : DemoApiResult
    data class Failure(val message: String) : DemoApiResult
}

interface DemoApiRepository {
    suspend fun checkProtectedEndpoint(): DemoApiResult
}
