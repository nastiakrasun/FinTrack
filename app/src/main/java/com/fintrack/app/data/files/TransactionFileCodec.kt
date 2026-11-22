package com.fintrack.app.data.files

import com.fintrack.app.domain.model.Transaction
import com.fintrack.app.domain.model.TransactionType
import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.google.gson.reflect.TypeToken
import java.time.LocalDate

object TransactionFileCodec {
    private val gson = Gson()
    private const val HEADER =
        "id,description,amountMinorUnits,type,categoryId,date,attachmentUri"
    private const val MAX_TRANSACTIONS = 5_000

    fun toJson(transactions: List<Transaction>): String =
        gson.toJson(transactions.map(TransactionFileRow::from))

    fun fromJson(json: String): List<Transaction> {
        try {
            val type = object : TypeToken<List<TransactionFileRow>>() {}.type
            val rows: List<TransactionFileRow> =
                gson.fromJson(json, type) ?: throw IllegalArgumentException("JSON must contain a transaction array.")
            return rows.map(TransactionFileRow::toDomain).also(::validateTransactions)
        } catch (error: JsonParseException) {
            throw IllegalArgumentException("The selected JSON file is not a valid FinTrack transaction export.", error)
        } catch (error: RuntimeException) {
            throw IllegalArgumentException("The selected JSON file contains invalid transaction data.", error)
        }
    }

    fun toCsv(transactions: List<Transaction>): String = buildString {
        appendLine(HEADER)
        transactions.forEach { transaction ->
            val fields = listOf(
                transaction.id,
                transaction.description,
                transaction.amountMinorUnits.toString(),
                transaction.type.name,
                transaction.categoryId.orEmpty(),
                transaction.date.toString(),
                transaction.attachmentUri.orEmpty()
            )
            appendLine(fields.joinToString(",") { it.csvEscape() })
        }
    }

    fun fromCsv(csv: String): List<Transaction> {
        val rows = parseCsv(csv)
        require(rows.isNotEmpty() && rows.first() == HEADER.split(',')) {
            "The selected CSV file does not have a FinTrack transaction header."
        }
        return rows.drop(1)
            .filter { row -> row.any(String::isNotBlank) }
            .mapIndexed { index, row ->
                require(row.size == 7) { "CSV row ${index + 2} must contain seven columns." }
                try {
                    Transaction(
                        id = row[0],
                        description = row[1],
                        amountMinorUnits = row[2].toLong(),
                        type = TransactionType.valueOf(row[3]),
                        categoryId = row[4].ifBlank { null },
                        date = LocalDate.parse(row[5]),
                        attachmentUri = row[6].ifBlank { null }
                    )
                } catch (error: RuntimeException) {
                    throw IllegalArgumentException("CSV row ${index + 2} contains invalid transaction data.", error)
                }
            }.also(::validateTransactions)
    }

    private fun validateTransactions(transactions: List<Transaction>) {
        require(transactions.size <= MAX_TRANSACTIONS) {
            "A FinTrack file may contain at most $MAX_TRANSACTIONS transactions."
        }
        require(transactions.all { it.id.isNotBlank() && it.description.isNotBlank() }) {
            "Each transaction must have an id and description."
        }
        require(transactions.all { it.amountMinorUnits > 0L }) {
            "Transaction amounts must be greater than zero."
        }
    }

    private fun String.csvEscape(): String =
        if (any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"${replace("\"", "\"\"")}\""
        } else {
            this
        }

    private fun parseCsv(input: String): List<List<String>> {
        val rows = mutableListOf<MutableList<String>>()
        var row = mutableListOf<String>()
        val cell = StringBuilder()
        var inQuotes = false
        var index = 0

        while (index < input.length) {
            val character = input[index]
            when {
                character == '"' && inQuotes && input.getOrNull(index + 1) == '"' -> {
                    cell.append('"')
                    index++
                }
                character == '"' -> inQuotes = !inQuotes
                character == ',' && !inQuotes -> {
                    row += cell.toString()
                    cell.clear()
                }
                (character == '\n' || character == '\r') && !inQuotes -> {
                    row += cell.toString()
                    cell.clear()
                    rows += row
                    row = mutableListOf()
                    if (character == '\r' && input.getOrNull(index + 1) == '\n') index++
                }
                else -> cell.append(character)
            }
            index++
        }
        require(!inQuotes) { "CSV contains an unterminated quoted field." }
        if (cell.isNotEmpty() || row.isNotEmpty()) {
            row += cell.toString()
            rows += row
        }
        return rows
    }

    private data class TransactionFileRow(
        val id: String,
        val description: String,
        val amountMinorUnits: Long,
        val type: String,
        val categoryId: String?,
        val date: String,
        val attachmentUri: String?
    ) {
        fun toDomain(): Transaction = try {
            Transaction(
                id = id,
                description = description,
                amountMinorUnits = amountMinorUnits,
                type = TransactionType.valueOf(type),
                categoryId = categoryId,
                date = LocalDate.parse(date),
                attachmentUri = attachmentUri
            )
        } catch (error: RuntimeException) {
            throw IllegalArgumentException("JSON contains invalid transaction data.", error)
        }

        companion object {
            fun from(transaction: Transaction) = TransactionFileRow(
                id = transaction.id,
                description = transaction.description,
                amountMinorUnits = transaction.amountMinorUnits,
                type = transaction.type.name,
                categoryId = transaction.categoryId,
                date = transaction.date.toString(),
                attachmentUri = transaction.attachmentUri
            )
        }

    }
}
