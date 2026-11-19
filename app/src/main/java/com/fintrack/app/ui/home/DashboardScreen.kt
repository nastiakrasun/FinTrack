package com.fintrack.app.ui.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.fintrack.app.domain.model.BatteryReading
import com.fintrack.app.domain.model.TransactionType
import java.text.NumberFormat
import java.util.Locale

@Composable
fun DashboardScreen(
    email: String,
    state: HomeUiState,
    onLogout: () -> Unit,
    onOpenApiDemo: () -> Unit,
    onBatteryAlertsEnabled: (Boolean) -> Unit
) {
    val currencyFormat = remember {
        NumberFormat.getCurrencyInstance(Locale.forLanguageTag("nb-NO"))
    }
    val context = LocalContext.current
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> onBatteryAlertsEnabled(granted) }

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
                        text = currencyFormat.format(state.balanceMinorUnits / 100.0),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Monthly spending", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(12.dp))
                    MonthlySpendingChart(monthlySpendingPeriods(state.transactions, state.month))
                }
            }
        }
        item {
            BatterySensorCard(
                reading = state.batteryReading,
                alertsEnabled = state.batteryAlertsEnabled,
                onEnableAlerts = {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        onBatteryAlertsEnabled(true)
                    } else {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            )
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("API and token demo", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Try a protected REST request using the current mock session token.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = onOpenApiDemo) {
                        Text("Open API demo")
                    }
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
                    amount = state.incomeMinorUnits,
                    modifier = Modifier.weight(1f),
                    currencyFormat = currencyFormat
                )
                SummaryCard(
                    title = "Expenses",
                    amount = state.expensesMinorUnits,
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
                        currencyFormat.format(state.remainingBudgetMinorUnits / 100.0),
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
        items(state.transactions, key = { it.id }) { transaction ->
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
private fun BatterySensorCard(
    reading: BatteryReading?,
    alertsEnabled: Boolean,
    onEnableAlerts: () -> Unit
) {
    val temperature = reading?.temperatureCelsius
    val voltage = reading?.voltageMillivolts
    val current = reading?.currentMicroamps

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Device battery sensors", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "Live device readings (not financial sensor data)",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                BatterySensorGauge(
                    label = "Temp",
                    value = temperature?.let { String.format(Locale.getDefault(), "%.1f°", it) } ?: "—",
                    progress = temperature?.div(60f)
                )
                BatterySensorGauge(
                    label = "Voltage",
                    value = voltage?.let { String.format(Locale.getDefault(), "%.2fV", it / 1000f) } ?: "—",
                    progress = voltage?.div(5000f)
                )
                BatterySensorGauge(
                    label = "Current",
                    value = current?.let { String.format(Locale.getDefault(), "%.0fmA", it / 1000f) } ?: "—",
                    progress = current?.let { kotlin.math.abs(it.toFloat()) / 3_000_000f }
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                reading?.let {
                    buildString {
                        append(it.levelPercent?.let { level -> "Battery $level%" } ?: "Battery level unavailable")
                        if (it.isCharging) append(" · Charging")
                    }
                } ?: "Waiting for battery sensor…",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(8.dp))
            Button(onClick = onEnableAlerts, enabled = !alertsEnabled) {
                Text(if (alertsEnabled) "High-temperature alerts enabled" else "Enable temperature alert")
            }
            if (temperature != null && temperature >= 45f) {
                Text(
                    "Battery is hot. Let the device cool down.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
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
