package com.fintrack.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fintrack.app.domain.model.Transaction
import com.fintrack.app.domain.model.TransactionType
import com.fintrack.app.domain.repository.FinanceRepository
import com.fintrack.app.domain.usecase.GetMonthlyOverviewUseCase
import java.text.NumberFormat
import java.time.YearMonth
import java.util.Locale

@Composable
fun DashboardScreen(
    email: String,
    financeRepository: FinanceRepository,
    transactions: List<Transaction>,
    onLogout: () -> Unit
) {
    val month = YearMonth.now()
    val overview = remember(transactions) { GetMonthlyOverviewUseCase(financeRepository)(month) }
    val currencyFormat = remember {
        NumberFormat.getCurrencyInstance(Locale.forLanguageTag("nb-NO"))
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 28.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("FinTrack", style = MaterialTheme.typography.headlineMedium)
                    Text(email, style = MaterialTheme.typography.bodyMedium)
                }
                Button(onClick = onLogout) {
                    Text("Log out")
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp)) {
                    Text("Monthly balance", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = currencyFormat.format(
                            (overview.incomeMinorUnits - overview.expensesMinorUnits) / 100.0
                        ),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SummaryCard(
                    title = "Income",
                    amount = overview.incomeMinorUnits,
                    modifier = Modifier.weight(1f),
                    currencyFormat = currencyFormat
                )
                SummaryCard(
                    title = "Expenses",
                    amount = overview.expensesMinorUnits,
                    modifier = Modifier.weight(1f),
                    currencyFormat = currencyFormat
                )
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Remaining budget", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        currencyFormat.format(overview.remainingBudgetMinorUnits / 100.0),
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }
        }
        item {
            Text(
                text = "Recent transactions",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        items(transactions.sortedByDescending(Transaction::date)) { transaction ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(transaction.description, fontWeight = FontWeight.Medium)
                        Text(
                            transaction.date.toString(),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    val sign = if (transaction.type == TransactionType.EXPENSE) "-" else "+"
                    Text(
                        "$sign${currencyFormat.format(transaction.amountMinorUnits / 100.0)}"
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(
    title: String,
    amount: Long,
    modifier: Modifier,
    currencyFormat: NumberFormat
) {
    Card(modifier = modifier) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(4.dp))
            Text(
                currencyFormat.format(amount / 100.0),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
