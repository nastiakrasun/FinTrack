package com.fintrack.app.data.remote

import retrofit2.http.GET

data class BearerResponse(
    val authenticated: Boolean,
    val token: String?
)

interface DemoApiService {
    @GET("bearer")
    suspend fun checkBearerToken(): BearerResponse
}
