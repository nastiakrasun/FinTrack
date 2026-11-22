package com.fintrack.app.ui.transactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fintrack.app.domain.model.Category
import com.fintrack.app.domain.model.Transaction
import com.fintrack.app.domain.model.TransactionType
import java.text.NumberFormat
import java.util.Locale

@Composable
fun TransactionsScreen(
    transactions: List<Transaction>,
    categories: List<Category>,
    onAddTransaction: () -> Unit
) {
    val currencyFormat = remember {
        NumberFormat.getCurrencyInstance(Locale.forLanguageTag("nb-NO"))
    }
    val categoryNames = remember(categories) { categories.associate { it.id to it.name } }
    // Reversed first so the most recently added item comes first among the same date.
    val sortedTransactions = remember(transactions) {
        transactions.asReversed().sortedByDescending(Transaction::date)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, top = 28.dp, end = 20.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("Transactions", style = MaterialTheme.typography.headlineMedium)
            }
            if (sortedTransactions.isEmpty()) {
                item {
                    Text(
                        text = "Your transactions will appear here",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
            items(sortedTransactions, key = Transaction::id) { transaction ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(transaction.description, fontWeight = FontWeight.Medium)
                            val categoryName = categoryNames[transaction.categoryId]
                            Text(
                                text = listOfNotNull(transaction.date.toString(), categoryName)
                                    .joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        val sign = if (transaction.type == TransactionType.EXPENSE) "-" else "+"
                        Text("$sign${currencyFormat.format(transaction.amountMinorUnits / 100.0)}")
                    }
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = onAddTransaction,
            icon = { Icon(Icons.Filled.Add, contentDescription = null) },
            text = { Text("Add transaction") },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        )
    }
}
