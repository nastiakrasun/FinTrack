package com.fintrack.app.data.remote

interface DemoApiDataSource {
    suspend fun checkBearerToken(): BearerResponse
}

class RetrofitDemoApiDataSource(
    private val service: DemoApiService
) : DemoApiDataSource {
    override suspend fun checkBearerToken(): BearerResponse = service.checkBearerToken()
}
