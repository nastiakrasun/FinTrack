package com.fintrack.app.data.repository

import com.fintrack.app.domain.model.Budget
import com.fintrack.app.domain.model.Category
import com.fintrack.app.domain.model.CategoryType
import com.fintrack.app.domain.model.Transaction
import com.fintrack.app.domain.model.TransactionType
import com.fintrack.app.domain.repository.FinanceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.YearMonth

class MockFinanceRepository(
    private val demoMonth: YearMonth = YearMonth.now()
) : FinanceRepository {

    private val categories = MutableStateFlow(listOf(
        Category(id = "groceries", name = "Groceries", type = CategoryType.EXPENSE),
        Category(id = "transport", name = "Transport", type = CategoryType.EXPENSE),
        Category(id = "subscriptions", name = "Subscriptions", type = CategoryType.EXPENSE),
        Category(id = "dining", name = "Dining", type = CategoryType.EXPENSE),
        Category(id = "salary", name = "Salary", type = CategoryType.INCOME)
    ))

    private val transactions = MutableStateFlow(
        listOf(
            transaction("salary", "Salary", 42_000_00, TransactionType.INCOME, "salary", 1),
            transaction("rema-1", "REMA 1000", 684_50, TransactionType.EXPENSE, "groceries", 3),
            transaction("vy-1", "Vy", 429_00, TransactionType.EXPENSE, "transport", 5),
            transaction("netflix", "Netflix", 129_00, TransactionType.EXPENSE, "subscriptions", 7),
            transaction("kiwi", "KIWI", 512_30, TransactionType.EXPENSE, "groceries", 10),
            transaction("circle-k", "Circle K", 799_00, TransactionType.EXPENSE, "transport", 12),
            transaction("spotify", "Spotify", 119_00, TransactionType.EXPENSE, "subscriptions", 14),
            transaction("cafe", "Local cafe", 186_00, TransactionType.EXPENSE, "dining", 16)
        )
    )

    private val budgets = MutableStateFlow(listOf(
        Budget("groceries-monthly", "groceries", demoMonth, 5_000_00),
        Budget("transport-monthly", "transport", demoMonth, 1_500_00),
        Budget("subscriptions-monthly", "subscriptions", demoMonth, 700_00),
        Budget("dining-monthly", "dining", demoMonth, 2_000_00)
    ))

    override fun observeTransactions(): Flow<List<Transaction>> = transactions.asStateFlow()

    override fun observeCategories(): Flow<List<Category>> = categories.asStateFlow()

    override fun observeBudgets(): Flow<List<Budget>> = budgets.asStateFlow()

    override suspend fun getTransactions(): List<Transaction> = transactions.value

    override suspend fun getTransaction(id: String): Transaction? =
        transactions.value.firstOrNull { it.id == id }

    override suspend fun getCategories(): List<Category> = categories.value

    override suspend fun getBudgets(): List<Budget> = budgets.value

    override suspend fun addTransaction(transaction: Transaction) {
        transactions.update { it + transaction }
    }

    override suspend fun updateTransaction(transaction: Transaction) {
        transactions.update { list -> list.map { if (it.id == transaction.id) transaction else it } }
    }

    override suspend fun deleteTransaction(id: String) {
        transactions.update { list -> list.filterNot { it.id == id } }
    }

    override suspend fun saveBudget(budget: Budget) {
        budgets.update { list -> list.filterNot { it.id == budget.id } + budget }
    }

    private fun transaction(
        id: String,
        description: String,
        amountMinorUnits: Long,
        type: TransactionType,
        categoryId: String?,
        dayOfMonth: Int
    ) = Transaction(
        id = id,
        description = description,
        amountMinorUnits = amountMinorUnits,
        type = type,
        categoryId = categoryId,
        date = demoMonth.atDay(dayOfMonth)
    )
}
