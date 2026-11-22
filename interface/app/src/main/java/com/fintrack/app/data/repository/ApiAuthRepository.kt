package com.fintrack.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.fintrack.app.data.remote.AuthApi
import com.fintrack.app.data.remote.LoginRequest
import com.fintrack.app.data.remote.RegisterRequest
import com.fintrack.app.domain.repository.AuthRepository
import com.fintrack.app.domain.repository.AuthResult
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.Locale

class ApiAuthRepository(context: Context) : AuthRepository {
    private val preferences: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val api: AuthApi = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(
            OkHttpClient.Builder()
                .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                .build()
        )
        .addConverterFactory(GsonConverterFactory.create(GsonBuilder().create()))
        .build()
        .create(AuthApi::class.java)

    override fun register(email: String, password: CharArray): AuthResult {
        val normalizedEmail = email.trim().lowercase(Locale.ROOT)
        val passwordText = String(password)
        password.fill('\u0000')
        return try {
            val response = api.register(
                RegisterRequest(normalizedEmail, passwordText)
            ).execute()
            if (response.isSuccessful) {
                val loginResult = login(normalizedEmail, passwordText.toCharArray())
                passwordText.toCharArray().fill('\u0000')
                loginResult
            } else if (response.code() == 409) {
                AuthResult.EMAIL_ALREADY_REGISTERED
            } else if (response.code() in 400..499) {
                AuthResult.INVALID_CREDENTIALS
            } else {
                AuthResult.NETWORK_ERROR
            }
        } catch (_: IOException) {
            AuthResult.NETWORK_ERROR
        }
    }

    override fun login(email: String, password: CharArray): AuthResult {
        val normalizedEmail = email.trim().lowercase(Locale.ROOT)
        val passwordText = String(password)
        password.fill('\u0000')
        return try {
            val response = api.login(LoginRequest(normalizedEmail, passwordText)).execute()
            passwordText.toCharArray().fill('\u0000')
            if (response.isSuccessful) {
                val token = response.body()?.accessToken
                if (token.isNullOrBlank()) {
                    AuthResult.NETWORK_ERROR
                } else {
                    preferences.edit().putString(KEY_ACCESS_TOKEN, token).apply()
                    AuthResult.SUCCESS
                }
            } else if (response.code() == 401 || response.code() == 400) {
                AuthResult.INVALID_CREDENTIALS
            } else {
                AuthResult.NETWORK_ERROR
            }
        } catch (_: IOException) {
            AuthResult.NETWORK_ERROR
        }
    }

    override fun logout() {
        preferences.edit().remove(KEY_ACCESS_TOKEN).apply()
    }

    companion object {
        private const val BASE_URL = "http://127.0.0.1:8080/"
        private const val PREFS_NAME = "fintrack_auth"
        private const val KEY_ACCESS_TOKEN = "access_token"
    }
}
