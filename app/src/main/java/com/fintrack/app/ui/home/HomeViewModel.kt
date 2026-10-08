package com.fintrack.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fintrack.app.domain.model.Transaction
import com.fintrack.app.domain.model.newestFirst
import com.fintrack.app.domain.repository.FinanceRepository
import com.fintrack.app.domain.usecase.GetMonthlyOverviewUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.YearMonth

data class HomeUiState(
    val balanceMinorUnits: Long = 0,
    val incomeMinorUnits: Long = 0,
    val expensesMinorUnits: Long = 0,
    val remainingBudgetMinorUnits: Long = 0,
    val transactions: List<Transaction> = emptyList()
)

class HomeViewModel(
    private val financeRepository: FinanceRepository,
    private val currentMonth: () -> YearMonth = { YearMonth.now() }
) : ViewModel() {

    private val getMonthlyOverview = GetMonthlyOverviewUseCase(financeRepository)

    val uiState: StateFlow<HomeUiState> = combine(
        getMonthlyOverview(currentMonth()),
        financeRepository.observeTransactions()
    ) { overview, transactions ->
        HomeUiState(
            balanceMinorUnits = overview.incomeMinorUnits - overview.expensesMinorUnits,
            incomeMinorUnits = overview.incomeMinorUnits,
            expensesMinorUnits = overview.expensesMinorUnits,
            remainingBudgetMinorUnits = overview.remainingBudgetMinorUnits,
            transactions = transactions.newestFirst()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = HomeUiState()
    )

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
