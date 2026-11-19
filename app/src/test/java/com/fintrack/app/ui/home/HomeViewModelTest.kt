package com.fintrack.app.ui.home

import com.fintrack.app.MainDispatcherRule
import com.fintrack.app.data.repository.MockFinanceRepository
import com.fintrack.app.domain.model.Transaction
import com.fintrack.app.domain.model.TransactionType
import com.fintrack.app.domain.model.BatteryReading
import com.fintrack.app.domain.repository.BatteryAlertNotifier
import com.fintrack.app.domain.repository.BatteryMonitorRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.time.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val month = YearMonth.of(2026, 9)
    private val repository = MockFinanceRepository(month)

    @Test
    fun exposesLiveBatteryReadingsAndSendsOneNotificationAtHighTemperature() = runTest {
        val reading = MutableStateFlow(
            BatteryReading(temperatureCelsius = 40f, voltageMillivolts = 4000, currentMicroamps = -500_000, levelPercent = 80, isCharging = true)
        )
        val notifications = mutableListOf<Float>()
        val monitor = object : BatteryMonitorRepository {
            override fun observeBattery(): Flow<BatteryReading> = reading
        }
        val notifier = object : BatteryAlertNotifier {
            override fun notifyHighTemperature(temperatureCelsius: Float) {
                notifications += temperatureCelsius
            }
        }
        val viewModel = HomeViewModel(
            financeRepository = repository,
            currentMonth = { month },
            batteryMonitorRepository = monitor,
            batteryAlertNotifier = notifier
        )
        collectedState(viewModel)

        assertEquals(40f, viewModel.uiState.value.batteryReading?.temperatureCelsius)
        viewModel.setBatteryAlertsEnabled(true)
        reading.value = reading.value.copy(temperatureCelsius = 46f)
        reading.value = reading.value.copy(temperatureCelsius = 47f)

        assertEquals(listOf(46f), notifications)
        assertEquals(4000, viewModel.uiState.value.batteryReading?.voltageMillivolts)
    }

    // StateFlow built with WhileSubscribed only produces values while it is collected.
    private fun TestScope.collectedState(viewModel: HomeViewModel): HomeUiState {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        return viewModel.uiState.value
    }

    @Test
    fun exposesMonthlyTotalsFromTheUseCase() = runTest {
        val state = collectedState(HomeViewModel(repository, currentMonth = { month }))

        assertEquals(4_200_000L, state.incomeMinorUnits)
        assertEquals(285_880L, state.expensesMinorUnits)
        assertEquals(4_200_000L - 285_880L, state.balanceMinorUnits)
        assertEquals(634_120L, state.remainingBudgetMinorUnits)
    }

    @Test
    fun listsTransactionsNewestFirst() = runTest {
        val dates = collectedState(
            HomeViewModel(repository, currentMonth = { month })
        ).transactions.map { it.date }

        assertEquals(dates.sortedDescending(), dates)
        assertEquals(month.atDay(16), dates.first())
    }

    @Test
    fun stateIsRecalculatedWhenATransactionIsAdded() = runTest {
        val viewModel = HomeViewModel(repository, currentMonth = { month })
        collectedState(viewModel)

        repository.addTransaction(
            Transaction(
                id = "new",
                description = "Taxi",
                amountMinorUnits = 25_000,
                type = TransactionType.EXPENSE,
                categoryId = "transport",
                date = month.atDay(20)
            )
        )

        val state = viewModel.uiState.value
        assertEquals(285_880L + 25_000L, state.expensesMinorUnits)
        assertEquals(4_200_000L - 285_880L - 25_000L, state.balanceMinorUnits)
        assertEquals(634_120L - 25_000L, state.remainingBudgetMinorUnits)
        assertEquals("new", state.transactions.first().id)
    }
}
