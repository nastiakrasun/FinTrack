package com.fintrack.app.data.files

import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.fintrack.app.domain.model.Transaction
import com.fintrack.app.domain.model.TransactionType
import java.io.OutputStream
import java.time.YearMonth
import java.util.Locale

object TransactionPdfReport {
    fun write(
        output: OutputStream,
        transactions: List<Transaction>,
        month: YearMonth
    ) {
        val monthly = transactions
            .filter { YearMonth.from(it.date) == month }
            .sortedByDescending(Transaction::date)
        val income = monthly.filter { it.type == TransactionType.INCOME }.sumOf(Transaction::amountMinorUnits)
        val expenses = monthly.filter { it.type == TransactionType.EXPENSE }.sumOf(Transaction::amountMinorUnits)
        val document = PdfDocument()
        val regular = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 11f }
        val title = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val heading = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val rowsPerPage = 48
        val rowChunks = monthly.chunked(rowsPerPage).ifEmpty { listOf(emptyList()) }

        try {
            rowChunks.forEachIndexed { pageIndex, rows ->
                val pageNumber = pageIndex + 1
                val page = document.startPage(
                    PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                )
                val canvas = page.canvas
                var y = 44f
                canvas.drawText("FinTrack monthly report", 40f, y, title)
                y += 24f
                canvas.drawText(month.toString(), 40f, y, regular)
                y += 24f
                if (pageIndex == 0) {
                    canvas.drawText("Income: ${formatMinor(income)}", 40f, y, regular)
                    y += 18f
                    canvas.drawText("Expenses: ${formatMinor(expenses)}", 40f, y, regular)
                    y += 18f
                    canvas.drawText("Transactions", 40f, y, heading)
                    y += 20f
                } else {
                    canvas.drawText("Transactions (continued)", 40f, y, heading)
                    y += 20f
                }

                rows.forEach { transaction ->
                    val description = transaction.description.take(MAX_DESCRIPTION_LENGTH)
                    val sign = if (transaction.type == TransactionType.EXPENSE) "-" else "+"
                    val line = "${transaction.date}   $description   $sign${formatMinor(transaction.amountMinorUnits)}"
                    canvas.drawText(line, 40f, y, regular)
                    y += ROW_HEIGHT
                }
                canvas.drawText("Page $pageNumber of ${rowChunks.size}", 480f, 815f, regular)
                document.finishPage(page)
            }
            document.writeTo(output)
        } finally {
            document.close()
        }
    }

    private fun formatMinor(amount: Long): String =
        String.format(Locale.US, "%.2f", amount / 100.0)

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val ROW_HEIGHT = 14f
    private const val MAX_DESCRIPTION_LENGTH = 55
}
