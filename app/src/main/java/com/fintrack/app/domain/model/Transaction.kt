package com.fintrack.app.domain.model

import java.time.LocalDate

enum class TransactionType {
    INCOME,
    EXPENSE
}

data class Transaction(
    val id: String,
    val description: String,
    val amountMinorUnits: Long,
    val type: TransactionType,
    val categoryId: String?,
    val date: LocalDate
)
