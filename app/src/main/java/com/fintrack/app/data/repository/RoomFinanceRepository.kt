package com.fintrack.app.data.repository

import com.fintrack.app.data.local.dao.BudgetDao
import com.fintrack.app.data.local.dao.CategoryDao
import com.fintrack.app.data.local.dao.TransactionDao
import com.fintrack.app.data.local.mapper.toDomain
import com.fintrack.app.data.local.mapper.toEntity
import com.fintrack.app.domain.model.Budget
import com.fintrack.app.domain.model.Category
import com.fintrack.app.domain.model.Transaction
import com.fintrack.app.domain.repository.FinanceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomFinanceRepository(
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao,
    private val budgetDao: BudgetDao
) : FinanceRepository {

    override fun observeTransactions(): Flow<List<Transaction>> =
        transactionDao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeCategories(): Flow<List<Category>> =
        categoryDao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeBudgets(): Flow<List<Budget>> =
        budgetDao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getTransactions(): List<Transaction> =
        transactionDao.getAll().map { it.toDomain() }

    override suspend fun getTransaction(id: String): Transaction? =
        transactionDao.getById(id)?.toDomain()

    override suspend fun getCategories(): List<Category> =
        categoryDao.getAll().map { it.toDomain() }

    override suspend fun getBudgets(): List<Budget> =
        budgetDao.getAll().map { it.toDomain() }

    override suspend fun addTransaction(transaction: Transaction) =
        transactionDao.insert(transaction.toEntity())

    override suspend fun updateTransaction(transaction: Transaction) =
        transactionDao.update(transaction.toEntity())

    override suspend fun deleteTransaction(id: String) = transactionDao.deleteById(id)

    override suspend fun saveBudget(budget: Budget) = budgetDao.upsert(budget.toEntity())
}
