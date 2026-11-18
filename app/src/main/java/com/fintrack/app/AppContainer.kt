package com.fintrack.app

import android.content.Context
import com.fintrack.app.data.local.DefaultDataSeeder
import com.fintrack.app.data.local.FinTrackDatabase
import com.fintrack.app.data.network.BearerTokenInterceptor
import com.fintrack.app.data.network.InMemoryDemoTokenStore
import com.fintrack.app.data.remote.DemoApiService
import com.fintrack.app.data.remote.RetrofitDemoApiDataSource
import com.fintrack.app.data.repository.InMemoryDemoAuthRepository
import com.fintrack.app.data.repository.RemoteDemoApiRepository
import com.fintrack.app.data.repository.RoomFinanceRepository
import com.fintrack.app.domain.repository.AuthRepository
import com.fintrack.app.domain.repository.DemoApiRepository
import com.fintrack.app.domain.repository.FinanceRepository
import com.fintrack.app.domain.repository.SessionTokenStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

// Holds app-wide singletons so they survive Activity recreation (e.g. rotation).
class AppContainer(context: Context) {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val database: FinTrackDatabase = FinTrackDatabase.create(context)

    // Demo-only: credentials are never persisted in Room.
    val authRepository: AuthRepository = InMemoryDemoAuthRepository()
    val sessionTokenStore: SessionTokenStore = InMemoryDemoTokenStore()

    private val demoApiService: DemoApiService = Retrofit.Builder()
        .baseUrl(DEMO_API_BASE_URL)
        .client(
            OkHttpClient.Builder()
                .addInterceptor(BearerTokenInterceptor(sessionTokenStore))
                .build()
        )
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(DemoApiService::class.java)

    val demoApiRepository: DemoApiRepository = RemoteDemoApiRepository(
        RetrofitDemoApiDataSource(demoApiService)
    )

    val financeRepository: FinanceRepository = RoomFinanceRepository(
        transactionDao = database.transactionDao(),
        categoryDao = database.categoryDao(),
        budgetDao = database.budgetDao()
    )

    init {
        applicationScope.launch { DefaultDataSeeder(database).seedIfNeeded() }
    }

    private companion object {
        const val DEMO_API_BASE_URL = "https://httpbin.org/"
    }
}
