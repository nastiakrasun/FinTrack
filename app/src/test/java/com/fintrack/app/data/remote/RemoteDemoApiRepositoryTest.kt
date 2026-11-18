package com.fintrack.app.data.remote

import com.fintrack.app.data.network.BearerTokenInterceptor
import com.fintrack.app.data.repository.RemoteDemoApiRepository
import com.fintrack.app.domain.repository.DemoApiResult
import com.fintrack.app.domain.repository.SessionTokenStore
import com.google.gson.JsonSyntaxException
import java.io.IOException
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class RemoteDemoApiRepositoryTest {
    @Test
    fun retrofitSendsBearerTokenAndGsonParsesTheResponse() = runBlocking {
        val server = MockWebServer()
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"authenticated":true,"token":"fintrack-demo-access-token"}""")
        )
        val tokenStore = TestTokenStore("fintrack-demo-access-token")
        val service = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(
                OkHttpClient.Builder()
                    .addInterceptor(BearerTokenInterceptor(tokenStore))
                    .build()
            )
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(DemoApiService::class.java)

        try {
            val result = RemoteDemoApiRepository(RetrofitDemoApiDataSource(service))
                .checkProtectedEndpoint()
            val request = server.takeRequest()

            assertEquals("GET", request.method)
            assertEquals("/bearer", request.path)
            assertEquals("Bearer fintrack-demo-access-token", request.getHeader("Authorization"))
            assertEquals(
                DemoApiResult.Success(true, "fintrack-demo-access-token"),
                result
            )
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun reportsUnauthorizedResponseWithoutExposingServerDetails() = runBlocking {
        val dataSource = object : DemoApiDataSource {
            override suspend fun checkBearerToken(): BearerResponse {
                throw HttpException(
                    retrofit2.Response.error<BearerResponse>(
                        401,
                        "private server body".toResponseBody()
                    )
                )
            }
        }

        val result = RemoteDemoApiRepository(dataSource).checkProtectedEndpoint()

        assertTrue(result is DemoApiResult.Failure)
        assertEquals(
            "The demo API rejected the test token. Sign out and sign in again.",
            (result as DemoApiResult.Failure).message
        )
    }

    @Test
    fun reportsNetworkFailureInUserFriendlyLanguage() = runBlocking {
        val dataSource = object : DemoApiDataSource {
            override suspend fun checkBearerToken(): BearerResponse {
                throw IOException("connection refused")
            }
        }

        val result = RemoteDemoApiRepository(dataSource).checkProtectedEndpoint()

        assertEquals(
            DemoApiResult.Failure(
                "Could not reach the demo API. Check your internet connection and try again."
            ),
            result
        )
    }

    @Test
    fun reportsServerFailureWithoutLeakingResponseBody() = runBlocking {
        val dataSource = object : DemoApiDataSource {
            override suspend fun checkBearerToken(): BearerResponse {
                throw HttpException(
                    retrofit2.Response.error<BearerResponse>(
                        503,
                        "internal exception details".toResponseBody()
                    )
                )
            }
        }

        val result = RemoteDemoApiRepository(dataSource).checkProtectedEndpoint()

        assertEquals(
            DemoApiResult.Failure("The demo API is temporarily unavailable. Please try again later."),
            result
        )
    }

    @Test
    fun reportsMalformedJsonResponse() = runBlocking {
        val dataSource = object : DemoApiDataSource {
            override suspend fun checkBearerToken(): BearerResponse {
                throw JsonSyntaxException("invalid JSON")
            }
        }

        val result = RemoteDemoApiRepository(dataSource).checkProtectedEndpoint()

        assertEquals(
            DemoApiResult.Failure("The demo API returned an invalid response. Please try again later."),
            result
        )
    }

    private class TestTokenStore(
        private val token: String?
    ) : SessionTokenStore {
        override fun currentToken(): String? = token
        override fun activateDemoToken() = Unit
        override fun clear() = Unit
    }
}
