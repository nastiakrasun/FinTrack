package com.fintrack.app.ui.transactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Card
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
    searchQuery: String,
    typeFilter: TransactionTypeFilter,
    sortOrder: TransactionSortOrder,
    onSearchQueryChange: (String) -> Unit,
    onTypeFilterChange: (TransactionTypeFilter) -> Unit,
    onSortOrderChange: (TransactionSortOrder) -> Unit,
    onAddTransaction: () -> Unit,
    onOpenMap: () -> Unit
) {
    val currencyFormat = remember {
        NumberFormat.getCurrencyInstance(Locale.forLanguageTag("nb-NO"))
    }
    val categoryNames = remember(categories) { categories.associate { it.id to it.name } }
    var sortMenuExpanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, top = 28.dp, end = 20.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Transactions", style = MaterialTheme.typography.headlineMedium)
                    TextButton(onClick = onOpenMap) {
                        Icon(Icons.Filled.Map, contentDescription = null)
                        Text(" Map")
                    }
                }
            }
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Search transactions") },
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Filled.Search, contentDescription = null)
                    }
                )
            }
            item {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TransactionTypeFilter.entries.forEach { filter ->
                        FilterChip(
                            selected = typeFilter == filter,
                            onClick = { onTypeFilterChange(filter) },
                            label = {
                                Text(
                                    when (filter) {
                                        TransactionTypeFilter.ALL -> "All"
                                        TransactionTypeFilter.INCOME -> "Income"
                                        TransactionTypeFilter.EXPENSE -> "Expense"
                                    }
                                )
                            }
                        )
                    }
                    TextButton(onClick = { sortMenuExpanded = true }) {
                        Text(
                            when (sortOrder) {
                                TransactionSortOrder.NEWEST -> "Newest"
                                TransactionSortOrder.OLDEST -> "Oldest"
                                TransactionSortOrder.AMOUNT_HIGH_TO_LOW -> "Amount ↓"
                                TransactionSortOrder.AMOUNT_LOW_TO_HIGH -> "Amount ↑"
                            }
                        )
                        DropdownMenu(
                            expanded = sortMenuExpanded,
                            onDismissRequest = { sortMenuExpanded = false }
                        ) {
                            TransactionSortOrder.entries.forEach { order ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            when (order) {
                                                TransactionSortOrder.NEWEST -> "Newest first"
                                                TransactionSortOrder.OLDEST -> "Oldest first"
                                                TransactionSortOrder.AMOUNT_HIGH_TO_LOW -> "Amount: high to low"
                                                TransactionSortOrder.AMOUNT_LOW_TO_HIGH -> "Amount: low to high"
                                            }
                                        )
                                    },
                                    onClick = {
                                        onSortOrderChange(order)
                                        sortMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
            if (transactions.isEmpty()) {
                item {
                    Text(
                        text = "No transactions match these filters.",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
            items(transactions, key = Transaction::id) { transaction ->
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
