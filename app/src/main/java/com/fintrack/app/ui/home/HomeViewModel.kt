package com.fintrack.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fintrack.app.domain.model.BatteryReading
import com.fintrack.app.domain.model.Transaction
import com.fintrack.app.domain.model.newestFirst
import com.fintrack.app.domain.repository.BatteryAlertNotifier
import com.fintrack.app.domain.repository.BatteryMonitorRepository
import com.fintrack.app.domain.repository.EmptyBatteryMonitorRepository
import com.fintrack.app.domain.repository.NoOpBatteryAlertNotifier
import com.fintrack.app.domain.repository.FinanceRepository
import com.fintrack.app.domain.usecase.GetMonthlyOverviewUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import java.time.YearMonth

data class HomeUiState(
    val balanceMinorUnits: Long = 0,
    val incomeMinorUnits: Long = 0,
    val expensesMinorUnits: Long = 0,
    val remainingBudgetMinorUnits: Long = 0,
    val transactions: List<Transaction> = emptyList(),
    val month: YearMonth = YearMonth.now(),
    val batteryReading: BatteryReading? = null,
    val batteryAlertsEnabled: Boolean = false
)

class HomeViewModel(
    private val financeRepository: FinanceRepository,
    private val currentMonth: () -> YearMonth = { YearMonth.now() },
    batteryMonitorRepository: BatteryMonitorRepository = EmptyBatteryMonitorRepository,
    private val batteryAlertNotifier: BatteryAlertNotifier = NoOpBatteryAlertNotifier
) : ViewModel() {

    private val getMonthlyOverview = GetMonthlyOverviewUseCase(financeRepository)
    private val batteryAlertsEnabled = MutableStateFlow(false)
    private var highTemperatureAlertSent = false

    val uiState: StateFlow<HomeUiState> = combine(
        getMonthlyOverview(currentMonth()),
        financeRepository.observeTransactions(),
        batteryMonitorRepository.observeBattery(),
        batteryAlertsEnabled
    ) { overview, transactions, batteryReading, alertsEnabled ->
        HomeUiState(
            balanceMinorUnits = overview.incomeMinorUnits - overview.expensesMinorUnits,
            incomeMinorUnits = overview.incomeMinorUnits,
            expensesMinorUnits = overview.expensesMinorUnits,
            remainingBudgetMinorUnits = overview.remainingBudgetMinorUnits,
            transactions = transactions.newestFirst(),
            month = currentMonth(),
            batteryReading = batteryReading,
            batteryAlertsEnabled = alertsEnabled
        )
    }.onEach { state ->
        val temperature = state.batteryReading?.temperatureCelsius
        if (temperature != null && temperature < BATTERY_ALERT_RESET_CELSIUS) {
            highTemperatureAlertSent = false
        } else if (
            state.batteryAlertsEnabled &&
            temperature != null &&
            temperature >= HIGH_BATTERY_TEMPERATURE_CELSIUS &&
            !highTemperatureAlertSent
        ) {
            highTemperatureAlertSent = true
            batteryAlertNotifier.notifyHighTemperature(temperature)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = HomeUiState()
    )

    fun setBatteryAlertsEnabled(enabled: Boolean) {
        batteryAlertsEnabled.value = enabled
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
        const val HIGH_BATTERY_TEMPERATURE_CELSIUS = 45f
        const val BATTERY_ALERT_RESET_CELSIUS = 43f
    }
}
