package com.fintrack.app.domain.repository

import com.fintrack.app.domain.model.Budget
import com.fintrack.app.domain.model.Category
import com.fintrack.app.domain.model.Transaction

interface FinanceRepository {
    fun getTransactions(): List<Transaction>

    fun addTransaction(transaction: Transaction)

    fun getCategories(): List<Category>

    fun getBudgets(): List<Budget>
}
