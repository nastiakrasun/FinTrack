package com.fintrack.app.data.network

import com.fintrack.app.domain.repository.SessionTokenStore
import okhttp3.Interceptor
import okhttp3.Response

class BearerTokenInterceptor(
    private val tokenStore: SessionTokenStore
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val token = tokenStore.currentToken()
        val authenticatedRequest = if (token == null) {
            request
        } else {
            request.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        }
        return chain.proceed(authenticatedRequest)
    }
}
