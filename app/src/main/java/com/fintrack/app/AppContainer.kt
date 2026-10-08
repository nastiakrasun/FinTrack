package com.fintrack.app

import android.content.Context
import com.fintrack.app.data.local.DefaultDataSeeder
import com.fintrack.app.data.local.FinTrackDatabase
import com.fintrack.app.data.repository.InMemoryDemoAuthRepository
import com.fintrack.app.data.repository.RoomFinanceRepository
import com.fintrack.app.domain.repository.AuthRepository
import com.fintrack.app.domain.repository.FinanceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

// Holds app-wide singletons so they survive Activity recreation (e.g. rotation).
class AppContainer(context: Context) {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val database: FinTrackDatabase = FinTrackDatabase.create(context)

    // Demo-only: credentials are never persisted in Room.
    val authRepository: AuthRepository = InMemoryDemoAuthRepository()

    val financeRepository: FinanceRepository = RoomFinanceRepository(
        transactionDao = database.transactionDao(),
        categoryDao = database.categoryDao(),
        budgetDao = database.budgetDao()
    )

    init {
        applicationScope.launch { DefaultDataSeeder(database).seedIfNeeded() }
    }
}
