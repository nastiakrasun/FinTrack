package com.fintrack.app.ui.transactions

import com.fintrack.app.MainDispatcherRule
import com.fintrack.app.data.repository.MockFinanceRepository
import com.fintrack.app.domain.model.CategoryType
import com.fintrack.app.domain.model.TransactionType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class TransactionViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val today = LocalDate.of(2026, 9, 20)
    private val repository = MockFinanceRepository(YearMonth.of(2026, 9))
    // Lazy: the ViewModel must be created after the rule has installed the test Main dispatcher.
    private val viewModel by lazy {
        TransactionViewModel(repository, today = { today }, newId = { "new-id" })
    }
    private val state get() = viewModel.uiState.value

    private fun transactions() = runBlocking { repository.getTransactions() }

    private fun fillValidForm() {
        viewModel.onDescriptionChange("Coffee")
        viewModel.onAmountChange("45.50")
        viewModel.onCategoryChange("dining")
    }

    @Test
    fun startsWithEmptyExpenseFormDatedToday() {
        assertEquals("", state.form.description)
        assertEquals("", state.form.amount)
        assertEquals(TransactionType.EXPENSE, state.form.type)
        assertNull(state.form.categoryId)
        assertEquals(today, state.form.date)
        assertFalse(state.form.isSaved)
        assertEquals(transactions().size, state.transactions.size)
    }

    @Test
    fun availableCategoriesFollowSelectedType() {
        assertTrue(state.availableCategories.all { it.type == CategoryType.EXPENSE })

        viewModel.onTypeChange(TransactionType.INCOME)

        assertEquals(listOf("salary"), state.availableCategories.map { it.id })
    }

    @Test
    fun changingTypeResetsSelectedCategory() {
        viewModel.onCategoryChange("dining")

        viewModel.onTypeChange(TransactionType.INCOME)

        assertNull(state.form.categoryId)
    }

    @Test
    fun emptyFormReportsErrorsAndAddsNothing() {
        val before = transactions().size

        viewModel.saveTransaction()

        assertEquals("Enter a description.", state.form.errors.description)
        assertEquals("Enter an amount.", state.form.errors.amount)
        assertEquals("Select a category.", state.form.errors.category)
        assertNull(state.form.errors.date)
        assertFalse(state.form.isSaved)
        assertEquals(before, transactions().size)
    }

    @Test
    fun invalidAmountsAreRejected() {
        fillValidForm()

        viewModel.onAmountChange("abc")
        viewModel.saveTransaction()
        assertEquals("Amount must be a number, for example 125.50.", state.form.errors.amount)

        viewModel.onAmountChange("0")
        viewModel.saveTransaction()
        assertEquals("Amount must be greater than 0.", state.form.errors.amount)
        assertFalse(state.form.isSaved)
    }

    @Test
    fun editingAFieldClearsOnlyItsError() {
        viewModel.saveTransaction()

        viewModel.onDescriptionChange("Coffee")

        assertNull(state.form.errors.description)
        assertNotNull(state.form.errors.amount)
        assertNotNull(state.form.errors.category)
    }

    @Test
    fun validFormIsStoredWithMinorUnitsAndAppearsFirstInList() {
        fillValidForm()

        viewModel.saveTransaction()

        val saved = transactions().last()
        assertEquals("new-id", saved.id)
        assertEquals("Coffee", saved.description)
        assertEquals(4_550L, saved.amountMinorUnits)
        assertEquals(TransactionType.EXPENSE, saved.type)
        assertEquals("dining", saved.categoryId)
        assertEquals(today, saved.date)
        assertTrue(state.form.isSaved)
        assertEquals("new-id", state.transactions.first().id)
    }

    @Test
    fun savingTwiceDoesNotDuplicateTheTransaction() {
        fillValidForm()
        val before = transactions().size

        viewModel.saveTransaction()
        viewModel.saveTransaction()

        assertEquals(before + 1, transactions().size)
    }

    @Test
    fun transactionsAddedElsewhereAreReflectedInState() {
        val other = TransactionViewModel(repository, today = { today }, newId = { "other-id" })
        other.onDescriptionChange("Lunch")
        other.onAmountChange("99")
        other.onCategoryChange("dining")

        other.saveTransaction()

        assertEquals("other-id", state.transactions.first().id)
    }
}
