package com.fintrack.app.domain.usecase

import com.fintrack.app.data.repository.MockFinanceRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.YearMonth

class GetMonthlyOverviewUseCaseTest {
    private val month = YearMonth.of(2026, 9)

    @Test
    fun calculatesMonthlyTotalsAndCategoryExpenses() = runTest {
        val overview = GetMonthlyOverviewUseCase(MockFinanceRepository(month))(month).first()

        assertEquals(4_200_000L, overview.incomeMinorUnits)
        assertEquals(2_858_80L, overview.expensesMinorUnits)
        assertEquals(920_000L, overview.budgetLimitMinorUnits)
        assertEquals(634_120L, overview.remainingBudgetMinorUnits)
        assertEquals(1_196_80L, overview.expensesByCategory["groceries"])
        assertEquals(1_228_00L, overview.expensesByCategory["transport"])
    }

    @Test
    fun returnsEmptyTotalsForMonthWithoutTransactionsOrBudgets() = runTest {
        val overview = GetMonthlyOverviewUseCase(MockFinanceRepository(month))(month.plusMonths(1)).first()

        assertEquals(0L, overview.incomeMinorUnits)
        assertEquals(0L, overview.expensesMinorUnits)
        assertEquals(0L, overview.budgetLimitMinorUnits)
        assertEquals(0L, overview.remainingBudgetMinorUnits)
        assertEquals(emptyMap<String, Long>(), overview.expensesByCategory)
    }
}
