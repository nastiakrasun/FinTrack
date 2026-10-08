package com.fintrack.app.ui.form

import com.fintrack.app.domain.model.Transaction
import com.fintrack.app.domain.model.TransactionType
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

const val MAX_DESCRIPTION_LENGTH = 100
const val MAX_AMOUNT_MINOR_UNITS = 1_000_000_000_00L

private val AMOUNT_PATTERN = Regex("-?\\d+([.,]\\d+)?")

data class TransactionFormInput(
    val description: String,
    val amount: String,
    val type: TransactionType,
    val categoryId: String?,
    val date: LocalDate?
)

data class TransactionFormErrors(
    val description: String? = null,
    val amount: String? = null,
    val category: String? = null,
    val date: String? = null
)

sealed interface TransactionFormResult {
    data class Valid(
        val description: String,
        val amountMinorUnits: Long,
        val type: TransactionType,
        val categoryId: String,
        val date: LocalDate
    ) : TransactionFormResult

    data class Invalid(val errors: TransactionFormErrors) : TransactionFormResult
}

sealed interface AmountParseResult {
    data class Success(val minorUnits: Long) : AmountParseResult
    data class Error(val message: String) : AmountParseResult
}

fun validateTransactionForm(input: TransactionFormInput): TransactionFormResult {
    val description = input.description.trim()
    val descriptionError = when {
        description.isEmpty() -> "Enter a description."
        description.length > MAX_DESCRIPTION_LENGTH ->
            "Description must be $MAX_DESCRIPTION_LENGTH characters or fewer."

        else -> null
    }
    val amount = parseAmountToMinorUnits(input.amount)
    val categoryId = input.categoryId
    val date = input.date

    if (descriptionError == null &&
        amount is AmountParseResult.Success &&
        categoryId != null &&
        date != null
    ) {
        return TransactionFormResult.Valid(
            description = description,
            amountMinorUnits = amount.minorUnits,
            type = input.type,
            categoryId = categoryId,
            date = date
        )
    }

    return TransactionFormResult.Invalid(
        TransactionFormErrors(
            description = descriptionError,
            amount = (amount as? AmountParseResult.Error)?.message,
            category = if (categoryId == null) "Select a category." else null,
            date = if (date == null) "Select a date." else null
        )
    )
}

// Converts user text such as "125.50" or "125,5" to minor units without using Double.
fun parseAmountToMinorUnits(text: String): AmountParseResult {
    val value = text.trim()
    if (value.isEmpty()) return AmountParseResult.Error("Enter an amount.")
    if (!AMOUNT_PATTERN.matches(value)) {
        return AmountParseResult.Error("Amount must be a number, for example 125.50.")
    }

    val normalized = value.replace(',', '.')
    if (normalized.substringAfter('.', "").length > 2) {
        return AmountParseResult.Error("Use at most 2 decimal places.")
    }

    val minorUnits = try {
        BigDecimal(normalized).movePointRight(2).longValueExact()
    } catch (_: ArithmeticException) {
        return AmountParseResult.Error("Amount is too large.")
    }
    return when {
        minorUnits <= 0 -> AmountParseResult.Error("Amount must be greater than 0.")
        minorUnits > MAX_AMOUNT_MINOR_UNITS -> AmountParseResult.Error("Amount is too large.")
        else -> AmountParseResult.Success(minorUnits)
    }
}

fun TransactionFormResult.Valid.toTransaction(
    id: String = UUID.randomUUID().toString()
) = Transaction(
    id = id,
    description = description,
    amountMinorUnits = amountMinorUnits,
    type = type,
    categoryId = categoryId,
    date = date
)
