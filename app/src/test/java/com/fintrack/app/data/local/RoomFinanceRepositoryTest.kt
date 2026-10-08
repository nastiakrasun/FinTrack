package com.fintrack.app.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.fintrack.app.data.repository.RoomFinanceRepository
import com.fintrack.app.domain.model.Budget
import com.fintrack.app.domain.model.Transaction
import com.fintrack.app.domain.model.TransactionType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.YearMonth

// In-memory Room database running on the JVM through Robolectric.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RoomFinanceRepositoryTest {
    private lateinit var database: FinTrackDatabase
    private lateinit var repository: RoomFinanceRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, FinTrackDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = RoomFinanceRepository(
            database.transactionDao(),
            database.categoryDao(),
            database.budgetDao()
        )
        runBlocking { DefaultDataSeeder(database).seedIfNeeded(MONTH) }
    }

    @After
    fun tearDown() = database.close()

    private fun transaction(
        id: String,
        amount: Long = 12_345,
        date: LocalDate = LocalDate.of(2026, 9, 10),
        type: TransactionType = TransactionType.EXPENSE,
        categoryId: String? = "groceries"
    ) = Transaction(id, "Item $id", amount, type, categoryId, date)

    @Test
    fun seedingCreatesDefaultCategoriesOnlyOnce() = runBlocking {
        val seeder = DefaultDataSeeder(database)
        seeder.seedIfNeeded(MONTH)
        seeder.seedIfNeeded(MONTH)

        val categories = repository.getCategories()
        assertEquals(DefaultDataSeeder.DEFAULT_CATEGORIES.map { it.id }, categories.map { it.id })
        assertEquals(7, categories.size)
        assertEquals(4, repository.getBudgets().size)
    }

    @Test
    fun insertedTransactionCanBeReadBackWithExactMinorUnits() = runBlocking {
        val saved = transaction("t1", amount = 4_550)

        repository.addTransaction(saved)

        assertEquals(saved, repository.getTransaction("t1"))
        assertEquals(listOf(saved), repository.getTransactions())
    }

    @Test
    fun transactionsAreOrderedByDateThenInsertion() = runBlocking {
        repository.addTransaction(transaction("late", date = LocalDate.of(2026, 9, 20)))
        repository.addTransaction(transaction("early", date = LocalDate.of(2026, 9, 1)))
        repository.addTransaction(transaction("early-2", date = LocalDate.of(2026, 9, 1)))

        assertEquals(listOf("early", "early-2", "late"), repository.getTransactions().map { it.id })
    }

    @Test
    fun updateChangesStoredTransaction() = runBlocking {
        repository.addTransaction(transaction("t1"))

        repository.updateTransaction(transaction("t1", amount = 99_00, categoryId = "transport"))

        val stored = repository.getTransaction("t1")!!
        assertEquals(99_00L, stored.amountMinorUnits)
        assertEquals("transport", stored.categoryId)
    }

    @Test
    fun deleteRemovesTransaction() = runBlocking {
        repository.addTransaction(transaction("t1"))

        repository.deleteTransaction("t1")

        assertNull(repository.getTransaction("t1"))
        assertEquals(emptyList<Transaction>(), repository.getTransactions())
    }

    @Test
    fun observeTransactionsEmitsUpdatedListAfterInsert() = runBlocking {
        assertEquals(emptyList<Transaction>(), repository.observeTransactions().first())

        repository.addTransaction(transaction("t1"))

        assertEquals(listOf("t1"), repository.observeTransactions().first().map { it.id })
    }

    @Test
    fun incomeTransactionKeepsItsType() = runBlocking {
        repository.addTransaction(transaction("pay", type = TransactionType.INCOME, categoryId = "salary"))

        assertEquals(TransactionType.INCOME, repository.getTransaction("pay")!!.type)
    }

    @Test
    fun savingBudgetWithSameIdUpdatesIt() = runBlocking {
        val id = "groceries-$MONTH"
        repository.saveBudget(Budget(id, "groceries", MONTH, 777_00))

        val budgets = repository.observeBudgets().first().filter { it.id == id }
        assertEquals(1, budgets.size)
        assertEquals(777_00L, budgets.single().limitMinorUnits)
    }

    private companion object {
        val MONTH: YearMonth = YearMonth.of(2026, 9)
    }
}
