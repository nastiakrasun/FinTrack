package com.fintrack.app.data.remote

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

data class RegisterRequest(
    val email: String,
    val password: String,
    val displayName: String? = null
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class UserResponse(
    val id: String,
    val email: String,
    val displayName: String?,
    val createdAtUtc: String
)

data class LoginResponse(
    val accessToken: String,
    val expiresAtUtc: String,
    val user: UserResponse
)

interface AuthApi {
    @POST("api/auth/register")
    fun register(@Body request: RegisterRequest): Call<UserResponse>

    @POST("api/auth/login")
    fun login(@Body request: LoginRequest): Call<LoginResponse>

    @GET("api/auth/me")
    fun me(@Header("Authorization") authorization: String): Call<UserResponse>
}
