package com.fintrack.app.data.local

import com.fintrack.app.data.local.entity.BudgetEntity
import com.fintrack.app.data.local.entity.CategoryEntity
import com.fintrack.app.domain.model.CategoryType
import java.time.YearMonth

// Fixed ids + IGNORE conflict strategy mean repeated app launches never create duplicates.
class DefaultDataSeeder(private val database: FinTrackDatabase) {

    suspend fun seedIfNeeded(month: YearMonth = YearMonth.now()) {
        database.categoryDao().insertAll(DEFAULT_CATEGORIES)
        // Starter budgets only on a fresh install, so a user's later edits/deletions are respected.
        if (database.budgetDao().count() == 0) {
            database.budgetDao().insertAll(defaultBudgets(month))
        }
    }

    private fun defaultBudgets(month: YearMonth) = DEFAULT_BUDGET_LIMITS.map { (categoryId, limit) ->
        BudgetEntity(
            id = "$categoryId-$month",
            categoryId = categoryId,
            month = month,
            limitMinorUnits = limit
        )
    }

    companion object {
        val DEFAULT_CATEGORIES = listOf(
            CategoryEntity("groceries", "Groceries", CategoryType.EXPENSE),
            CategoryEntity("transport", "Transport", CategoryType.EXPENSE),
            CategoryEntity("subscriptions", "Subscriptions", CategoryType.EXPENSE),
            CategoryEntity("salary", "Salary", CategoryType.INCOME),
            CategoryEntity("fuel", "Fuel", CategoryType.EXPENSE),
            CategoryEntity("entertainment", "Entertainment", CategoryType.EXPENSE),
            CategoryEntity("other", "Other", CategoryType.EXPENSE)
        )

        private val DEFAULT_BUDGET_LIMITS = listOf(
            "groceries" to 5_000_00L,
            "transport" to 1_500_00L,
            "subscriptions" to 700_00L,
            "entertainment" to 1_000_00L
        )
    }
}
