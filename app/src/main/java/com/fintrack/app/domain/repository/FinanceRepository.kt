package com.fintrack.app.domain.repository

import com.fintrack.app.domain.model.Budget
import com.fintrack.app.domain.model.Category
import com.fintrack.app.domain.model.Transaction
import kotlinx.coroutines.flow.Flow

interface FinanceRepository {
    fun observeTransactions(): Flow<List<Transaction>>

    fun observeCategories(): Flow<List<Category>>

    fun observeBudgets(): Flow<List<Budget>>

    suspend fun getTransactions(): List<Transaction>

    suspend fun getTransaction(id: String): Transaction?

    suspend fun getCategories(): List<Category>

    suspend fun getBudgets(): List<Budget>

    suspend fun addTransaction(transaction: Transaction)

    suspend fun updateTransaction(transaction: Transaction)

    suspend fun deleteTransaction(id: String)

    suspend fun saveBudget(budget: Budget)
}
