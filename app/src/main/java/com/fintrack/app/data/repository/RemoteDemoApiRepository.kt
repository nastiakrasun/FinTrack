package com.fintrack.app.data.repository

import com.google.gson.JsonParseException
import com.fintrack.app.data.remote.DemoApiDataSource
import com.fintrack.app.domain.repository.DemoApiRepository
import com.fintrack.app.domain.repository.DemoApiResult
import java.io.IOException
import retrofit2.HttpException

class RemoteDemoApiRepository(
    private val dataSource: DemoApiDataSource
) : DemoApiRepository {
    override suspend fun checkProtectedEndpoint(): DemoApiResult {
        return try {
            val response = dataSource.checkBearerToken()
            if (response.authenticated) {
                DemoApiResult.Success(
                    authenticated = true,
                    token = response.token
                )
            } else {
                DemoApiResult.Failure("The demo API rejected the test token. Sign out and sign in again.")
            }
        } catch (error: IOException) {
            DemoApiResult.Failure("Could not reach the demo API. Check your internet connection and try again.")
        } catch (error: HttpException) {
            val message = when {
                error.code() == 401 || error.code() == 403 ->
                    "The demo API rejected the test token. Sign out and sign in again."
                error.code() >= 500 ->
                    "The demo API is temporarily unavailable. Please try again later."
                else ->
                    "The demo API request failed (HTTP ${error.code()}). Please try again."
            }
            DemoApiResult.Failure(message)
        } catch (error: JsonParseException) {
            DemoApiResult.Failure("The demo API returned an invalid response. Please try again later.")
        }
    }
}
