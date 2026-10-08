package com.fintrack.app.ui.form

import com.fintrack.app.data.repository.MockFinanceRepository
import com.fintrack.app.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class TransactionValidationTest {
    private val date = LocalDate.of(2026, 10, 8)

    private fun input(
        description: String = "Coffee",
        amount: String = "45.50",
        type: TransactionType = TransactionType.EXPENSE,
        categoryId: String? = "dining",
        date: LocalDate? = this.date
    ) = TransactionFormInput(description, amount, type, categoryId, date)

    private fun minorUnits(text: String): Long =
        (parseAmountToMinorUnits(text) as AmountParseResult.Success).minorUnits

    private fun amountError(text: String): String =
        (parseAmountToMinorUnits(text) as AmountParseResult.Error).message

    @Test
    fun convertsDecimalAmountsToMinorUnitsExactly() {
        assertEquals(12_550L, minorUnits("125.50"))
        assertEquals(12_550L, minorUnits("125,5"))
        assertEquals(10_000L, minorUnits("100"))
        assertEquals(1L, minorUnits("0.01"))
        assertEquals(1_999L, minorUnits(" 19.99 "))
    }

    @Test
    fun rejectsEmptyAndNonNumericAmounts() {
        assertEquals("Enter an amount.", amountError(""))
        listOf("abc", "12a", "1e3", "12.", ".5", "1.2.3").forEach {
            assertEquals("Amount must be a number, for example 125.50.", amountError(it))
        }
    }

    @Test
    fun rejectsZeroAndNegativeAmounts() {
        listOf("0", "0.00", "-5", "-0").forEach {
            assertEquals("Amount must be greater than 0.", amountError(it))
        }
    }

    @Test
    fun rejectsTooManyDecimalsAndTooLargeAmounts() {
        assertEquals("Use at most 2 decimal places.", amountError("1.234"))
        assertEquals("Amount is too large.", amountError("1000000001"))
        assertEquals("Amount is too large.", amountError("99999999999999999999"))
    }

    @Test
    fun validFormProducesTransactionWithMinorUnits() {
        val result = validateTransactionForm(input(description = "  Coffee  "))

        val valid = result as TransactionFormResult.Valid
        val transaction = valid.toTransaction(id = "new")
        assertEquals("Coffee", transaction.description)
        assertEquals(4_550L, transaction.amountMinorUnits)
        assertEquals(TransactionType.EXPENSE, transaction.type)
        assertEquals("dining", transaction.categoryId)
        assertEquals(date, transaction.date)
    }

    @Test
    fun emptyFormReportsEveryFieldError() {
        val result = validateTransactionForm(input(description = " ", amount = "", categoryId = null, date = null))

        val errors = (result as TransactionFormResult.Invalid).errors
        assertEquals("Enter a description.", errors.description)
        assertEquals("Enter an amount.", errors.amount)
        assertEquals("Select a category.", errors.category)
        assertEquals("Select a date.", errors.date)
    }

    @Test
    fun zeroAmountIsInvalidWhileOtherFieldsAreValid() {
        val result = validateTransactionForm(input(amount = "0"))

        val errors = (result as TransactionFormResult.Invalid).errors
        assertEquals("Amount must be greater than 0.", errors.amount)
        assertEquals(TransactionFormErrors(amount = errors.amount), errors)
    }

    @Test
    fun rejectsTooLongDescription() {
        val result = validateTransactionForm(input(description = "x".repeat(MAX_DESCRIPTION_LENGTH + 1)))

        assertTrue(result is TransactionFormResult.Invalid)
    }

    @Test
    fun addedTransactionIsReturnedByRepository() {
        val repository = MockFinanceRepository()
        val before = repository.getTransactions().size
        val transaction = (validateTransactionForm(input()) as TransactionFormResult.Valid)
            .toTransaction(id = "new")

        repository.addTransaction(transaction)

        assertEquals(before + 1, repository.getTransactions().size)
        assertEquals(transaction, repository.getTransactions().last())
    }
}
