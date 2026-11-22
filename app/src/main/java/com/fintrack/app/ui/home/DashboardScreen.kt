package com.fintrack.app.ui.home

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.database.Cursor
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.fintrack.app.domain.model.BatteryReading
import com.fintrack.app.domain.model.Transaction
import com.fintrack.app.domain.model.TransactionType
import com.fintrack.app.data.files.TransactionFileCodec
import com.fintrack.app.data.files.TransactionPdfReport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.YearMonth
import java.text.NumberFormat
import java.util.Locale

@Composable
fun DashboardScreen(
    email: String,
    state: HomeUiState,
    onLogout: () -> Unit,
    onOpenApiDemo: () -> Unit,
    onBatteryAlertsEnabled: (Boolean) -> Unit,
    onImportTransactions: (List<Transaction>) -> Unit,
    onAttachFile: (String, String) -> Unit
) {
    val currencyFormat = remember {
        NumberFormat.getCurrencyInstance(Locale.forLanguageTag("nb-NO"))
    }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var fileMessage by remember { mutableStateOf<String?>(null) }
    var attachmentTargetId by remember { mutableStateOf<String?>(null) }
    val exportTransactions = state.transactions

    val exportCsvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use {
                        it.write(TransactionFileCodec.toCsv(exportTransactions))
                    } ?: error("Could not open the selected destination.")
                }
            }.onSuccess { fileMessage = "CSV report saved." }
                .onFailure { fileMessage = "Could not save CSV: ${it.message}" }
        }
    }
    val exportJsonLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use {
                        it.write(TransactionFileCodec.toJson(exportTransactions))
                    } ?: error("Could not open the selected destination.")
                }
            }.onSuccess { fileMessage = "JSON report saved." }
                .onFailure { fileMessage = "Could not save JSON: ${it.message}" }
        }
    }
    val pdfLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use {
                        TransactionPdfReport.write(it, exportTransactions, state.month)
                    } ?: error("Could not open the selected destination.")
                }
            }.onSuccess { fileMessage = "PDF report saved." }
                .onFailure { fileMessage = "Could not save PDF: ${it.message}" }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val content = context.contentResolver.openInputStream(uri)
                        ?.bufferedReader()?.use { it.readText() }
                        ?: error("Could not read the selected file.")
                    require(content.length <= MAX_IMPORT_CHARACTERS) { "File is too large to import." }
                    val filename = displayName(context, uri).lowercase()
                    if (filename.endsWith(".json") ||
                        context.contentResolver.getType(uri) == "application/json"
                    ) {
                        TransactionFileCodec.fromJson(content)
                    } else {
                        TransactionFileCodec.fromCsv(content)
                    }
                }
            }.onSuccess(onImportTransactions)
                .onFailure { fileMessage = "Could not import file: ${it.message}" }
        }
    }
    val photoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> saveAttachment(uri, attachmentTargetId, context, onAttachFile) { fileMessage = it } }
    val documentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> saveAttachment(uri, attachmentTargetId, context, onAttachFile) { fileMessage = it } }
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
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Reports and backups", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { exportCsvLauncher.launch("fintrack-transactions.csv") }) {
                            Text("CSV")
                        }
                        OutlinedButton(onClick = { exportJsonLauncher.launch("fintrack-transactions.json") }) {
                            Text("JSON")
                        }
                        OutlinedButton(onClick = { pdfLauncher.launch("fintrack-report.pdf") }) {
                            Text("PDF")
                        }
                    }
                    OutlinedButton(onClick = {
                        importLauncher.launch(arrayOf("text/*", "application/json"))
                    }) {
                        Text("Import CSV / JSON")
                    }
                    (state.fileOperationMessage ?: fileMessage)?.let { message ->
                        Text(message, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
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
                    AnimatedContent(
                        targetState = state.balanceMinorUnits,
                        label = "monthly-balance"
                    ) { balance ->
                        Text(
                            text = currencyFormat.format(balance / 100.0),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        item {
            DeviceResourcesCard(state.deviceResources)
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
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(transaction.description, fontWeight = FontWeight.Medium)
                            Text(transaction.date.toString(), style = MaterialTheme.typography.bodySmall)
                        }
                        val sign = if (transaction.type == TransactionType.EXPENSE) "-" else "+"
                        Text("$sign${currencyFormat.format(transaction.amountMinorUnits / 100.0)}")
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = {
                            attachmentTargetId = transaction.id
                            photoLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }) { Text("Photo") }
                        OutlinedButton(onClick = {
                            attachmentTargetId = transaction.id
                            documentLauncher.launch(arrayOf("application/pdf", "image/*", "text/plain"))
                        }) { Text("Document") }
                        transaction.attachmentUri?.let { attachment ->
                            OutlinedButton(onClick = {
                                runCatching {
                                    val mime = context.contentResolver.getType(Uri.parse(attachment)) ?: "*/*"
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, Uri.parse(attachment))
                                            .setDataAndType(Uri.parse(attachment), mime)
                                            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    )
                                }.onFailure {
                                    fileMessage = if (it is ActivityNotFoundException) {
                                        "No app can open this attachment."
                                    } else {
                                        "Could not open attachment: ${it.message}"
                                    }
                                }
                            }) { Text("View file") }
                        }
                    }
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
private fun DeviceResourcesCard(reading: com.fintrack.app.domain.model.DeviceResourceReading?) {
    val cpu = reading?.processCpuPercent?.let { String.format(Locale.getDefault(), "%.1f%%", it) } ?: "—"
    val memory = if (reading?.memoryUsedMegabytes != null && reading.memoryTotalMegabytes != null) {
        "${reading.memoryUsedMegabytes} / ${reading.memoryTotalMegabytes} MB"
    } else {
        "—"
    }
    val network = if (
        reading?.networkReceivedBytesPerSecond != null &&
        reading.networkSentBytesPerSecond != null
    ) {
        "${formatBytesPerSecond(reading.networkReceivedBytesPerSecond)} ↓  " +
            "${formatBytesPerSecond(reading.networkSentBytesPerSecond)} ↑"
    } else {
        "Unavailable"
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Device resources · updates every 2 sec", style = MaterialTheme.typography.titleMedium)
            Text("App CPU: $cpu")
            Text("Memory used: $memory")
            Text("Network RX / TX: $network", style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun formatBytesPerSecond(value: Long): String = when {
    value >= 1_000_000 -> String.format(Locale.getDefault(), "%.1f MB/s", value / 1_000_000f)
    value >= 1_000 -> String.format(Locale.getDefault(), "%.1f KB/s", value / 1_000f)
    else -> "$value B/s"
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

private fun saveAttachment(
    uri: Uri?,
    transactionId: String?,
    context: android.content.Context,
    onAttachFile: (String, String) -> Unit,
    onError: (String) -> Unit
) {
    if (uri == null) return
    if (transactionId == null) {
        onError("Choose a transaction before selecting a file.")
        return
    }
    runCatching {
        context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
    }.onFailure {
        onError("The file can be attached for this session, but its access may not survive restart.")
    }
    onAttachFile(transactionId, uri.toString())
}

private fun displayName(context: android.content.Context, uri: Uri): String {
    var cursor: Cursor? = null
    return try {
        cursor = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        if (cursor != null && cursor.moveToFirst()) {
            cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
        } else {
            uri.lastPathSegment.orEmpty()
        }
    } finally {
        cursor?.close()
    }
}

private const val MAX_IMPORT_CHARACTERS = 5_000_000
