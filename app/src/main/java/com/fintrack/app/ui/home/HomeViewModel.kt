package com.fintrack.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fintrack.app.domain.model.BatteryReading
import com.fintrack.app.domain.model.DeviceResourceReading
import com.fintrack.app.domain.model.Transaction
import com.fintrack.app.domain.model.newestFirst
import com.fintrack.app.domain.repository.BatteryAlertNotifier
import com.fintrack.app.domain.repository.BatteryMonitorRepository
import com.fintrack.app.domain.repository.DeviceResourceMonitor
import com.fintrack.app.domain.repository.EmptyDeviceResourceMonitor
import com.fintrack.app.domain.repository.EmptyBatteryMonitorRepository
import com.fintrack.app.domain.repository.NoOpBatteryAlertNotifier
import com.fintrack.app.domain.repository.FinanceRepository
import com.fintrack.app.domain.usecase.GetMonthlyOverviewUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.YearMonth

data class HomeUiState(
    val balanceMinorUnits: Long = 0,
    val incomeMinorUnits: Long = 0,
    val expensesMinorUnits: Long = 0,
    val remainingBudgetMinorUnits: Long = 0,
    val transactions: List<Transaction> = emptyList(),
    val month: YearMonth = YearMonth.now(),
    val batteryReading: BatteryReading? = null,
    val batteryAlertsEnabled: Boolean = false,
    val deviceResources: DeviceResourceReading? = null,
    val fileOperationMessage: String? = null
)

class HomeViewModel(
    private val financeRepository: FinanceRepository,
    private val currentMonth: () -> YearMonth = { YearMonth.now() },
    batteryMonitorRepository: BatteryMonitorRepository = EmptyBatteryMonitorRepository,
    private val batteryAlertNotifier: BatteryAlertNotifier = NoOpBatteryAlertNotifier,
    deviceResourceMonitor: DeviceResourceMonitor = EmptyDeviceResourceMonitor
) : ViewModel() {

    private val getMonthlyOverview = GetMonthlyOverviewUseCase(financeRepository)
    private val batteryAlertsEnabled = MutableStateFlow(false)
    private val _fileOperationMessage = MutableStateFlow<String?>(null)
    private var highTemperatureAlertSent = false

    private val financeAndDeviceState = combine(
        getMonthlyOverview(currentMonth()),
        financeRepository.observeTransactions(),
        batteryMonitorRepository.observeBattery(),
        batteryAlertsEnabled,
        deviceResourceMonitor.observeResources(),
    ) { overview, transactions, batteryReading, alertsEnabled, resources ->
        HomeUiState(
            balanceMinorUnits = overview.incomeMinorUnits - overview.expensesMinorUnits,
            incomeMinorUnits = overview.incomeMinorUnits,
            expensesMinorUnits = overview.expensesMinorUnits,
            remainingBudgetMinorUnits = overview.remainingBudgetMinorUnits,
            transactions = transactions.newestFirst(),
            month = currentMonth(),
            batteryReading = batteryReading,
            batteryAlertsEnabled = alertsEnabled,
            deviceResources = resources
        )
    }

    val uiState: StateFlow<HomeUiState> = combine(
        financeAndDeviceState,
        _fileOperationMessage
    ) { state, fileMessage -> state.copy(fileOperationMessage = fileMessage) }
        .onEach { state ->
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

    fun importTransactions(transactions: List<Transaction>) {
        viewModelScope.launch {
            _fileOperationMessage.value = "Importing ${transactions.size} transactions…"
            try {
                withContext(Dispatchers.IO) {
                    transactions.forEach { transaction ->
                        if (financeRepository.getTransaction(transaction.id) == null) {
                            financeRepository.addTransaction(transaction)
                        } else {
                            financeRepository.updateTransaction(transaction)
                        }
                    }
                }
                _fileOperationMessage.value = "Imported ${transactions.size} transactions."
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _fileOperationMessage.value = "Could not import transactions: ${error.message ?: "invalid file"}"
            }
        }
    }

    fun attachFile(transactionId: String, uri: String) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val transaction = financeRepository.getTransaction(transactionId)
                        ?: error("Transaction no longer exists.")
                    financeRepository.updateTransaction(transaction.copy(attachmentUri = uri))
                }
                _fileOperationMessage.value = "Receipt attached to transaction."
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _fileOperationMessage.value = "Could not attach the file: ${error.message ?: "unknown error"}"
            }
        }
    }

    fun clearFileOperationMessage() {
        _fileOperationMessage.value = null
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
        const val HIGH_BATTERY_TEMPERATURE_CELSIUS = 45f
        const val BATTERY_ALERT_RESET_CELSIUS = 43f
    }
}
