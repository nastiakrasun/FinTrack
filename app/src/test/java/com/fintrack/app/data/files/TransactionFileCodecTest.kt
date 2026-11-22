package com.fintrack.app.data.files

import com.fintrack.app.domain.model.Transaction
import com.fintrack.app.domain.model.TransactionType
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TransactionFileCodecTest {
    private val source = listOf(
        Transaction(
            id = "csv-1",
            description = "Cafe, \"Central\"\nsecond line",
            amountMinorUnits = 12_345,
            type = TransactionType.EXPENSE,
            categoryId = "dining",
            date = LocalDate.of(2026, 11, 19),
            attachmentUri = "content://receipts/one"
        ),
        Transaction(
            id = "json-2",
            description = "Salary",
            amountMinorUnits = 420_000,
            type = TransactionType.INCOME,
            categoryId = null,
            date = LocalDate.of(2026, 11, 1)
        )
    )

    @Test
    fun csvRoundTripPreservesQuotedCommasQuotesNewlinesAndAttachments() {
        val result = TransactionFileCodec.fromCsv(TransactionFileCodec.toCsv(source))

        assertEquals(source, result)
    }

    @Test
    fun jsonRoundTripPreservesDomainTransactions() {
        val result = TransactionFileCodec.fromJson(TransactionFileCodec.toJson(source))

        assertEquals(source, result)
    }

    @Test
    fun rejectsCsvWithoutFinTrackHeader() {
        val error = runCatching { TransactionFileCodec.fromCsv("id,amount\n1,100") }.exceptionOrNull()

        assertTrue(error is IllegalArgumentException)
    }

    @Test
    fun rejectsMalformedJson() {
        val error = runCatching { TransactionFileCodec.fromJson("{invalid") }.exceptionOrNull()

        assertTrue(error is IllegalArgumentException)
    }
}
