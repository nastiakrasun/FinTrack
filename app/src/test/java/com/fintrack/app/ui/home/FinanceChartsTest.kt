package com.fintrack.app.ui.home

import com.fintrack.app.domain.model.Transaction
import com.fintrack.app.domain.model.TransactionType
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Test

class FinanceChartsTest {
    @Test
    fun groupsMonthlyExpensesIntoFourPeriodsAndIgnoresIncomeAndOtherMonths() {
        val month = YearMonth.of(2026, 9)
        val transactions = listOf(
            transaction("expense-1", 1_000, TransactionType.EXPENSE, month.atDay(2)),
            transaction("expense-2", 2_500, TransactionType.EXPENSE, month.atDay(7)),
            transaction("income", 50_000, TransactionType.INCOME, month.atDay(8)),
            transaction("expense-3", 3_000, TransactionType.EXPENSE, month.atDay(15)),
            transaction("other-month", 90_000, TransactionType.EXPENSE, month.plusMonths(1).atDay(1))
        )

        val result = monthlySpendingPeriods(transactions, month)

        assertEquals(listOf("Week 1", "Week 2", "Week 3", "Week 4"), result.map { it.label })
        assertEquals(listOf(3_500L, 0L, 3_000L, 0L), result.map { it.amountMinorUnits })
    }

    private fun transaction(
        id: String,
        amount: Long,
        type: TransactionType,
        date: LocalDate
    ) = Transaction(id, id, amount, type, null, date)
}
