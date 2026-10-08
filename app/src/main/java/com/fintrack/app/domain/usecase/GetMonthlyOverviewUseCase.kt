package com.fintrack.app.domain.usecase

import com.fintrack.app.domain.model.Budget
import com.fintrack.app.domain.model.Transaction
import com.fintrack.app.domain.model.TransactionType
import com.fintrack.app.domain.repository.FinanceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.YearMonth

data class MonthlyOverview(
    val incomeMinorUnits: Long,
    val expensesMinorUnits: Long,
    val budgetLimitMinorUnits: Long,
    val remainingBudgetMinorUnits: Long,
    val expensesByCategory: Map<String, Long>
)

class GetMonthlyOverviewUseCase(
    private val financeRepository: FinanceRepository
) {
    // Re-emits whenever transactions or budgets change in the repository.
    operator fun invoke(month: YearMonth): Flow<MonthlyOverview> = combine(
        financeRepository.observeTransactions(),
        financeRepository.observeBudgets()
    ) { transactions, budgets -> calculate(month, transactions, budgets) }

    fun calculate(
        month: YearMonth,
        transactions: List<Transaction>,
        budgets: List<Budget>
    ): MonthlyOverview {
        val monthTransactions = transactions.filter { YearMonth.from(it.date) == month }
        val monthBudgets = budgets.filter { it.month == month }

        val income = monthTransactions.sumAmounts(TransactionType.INCOME)
        val expenses = monthTransactions.sumAmounts(TransactionType.EXPENSE)
        val budgetLimit = monthBudgets.sumOf(Budget::limitMinorUnits)

        return MonthlyOverview(
            incomeMinorUnits = income,
            expensesMinorUnits = expenses,
            budgetLimitMinorUnits = budgetLimit,
            remainingBudgetMinorUnits = budgetLimit - expenses,
            expensesByCategory = monthTransactions
                .asSequence()
                .filter { it.type == TransactionType.EXPENSE && it.categoryId != null }
                .groupBy { requireNotNull(it.categoryId) }
                .mapValues { (_, transactions) ->
                    transactions.sumOf(Transaction::amountMinorUnits)
                }
        )
    }

    private fun List<Transaction>.sumAmounts(type: TransactionType): Long =
        filter { it.type == type }.sumOf(Transaction::amountMinorUnits)
}
