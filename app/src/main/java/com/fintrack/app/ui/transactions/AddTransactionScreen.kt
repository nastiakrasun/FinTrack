package com.fintrack.app.ui.transactions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.fintrack.app.domain.model.Category
import com.fintrack.app.domain.model.CategoryType
import com.fintrack.app.domain.model.Transaction
import com.fintrack.app.domain.model.TransactionType
import com.fintrack.app.ui.components.FormTextField
import com.fintrack.app.ui.form.TransactionFormErrors
import com.fintrack.app.ui.form.TransactionFormInput
import com.fintrack.app.ui.form.TransactionFormResult
import com.fintrack.app.ui.form.toTransaction
import com.fintrack.app.ui.form.validateTransactionForm
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@Composable
fun AddTransactionScreen(
    categories: List<Category>,
    onSave: (Transaction) -> Unit,
    onBack: () -> Unit
) {
    var description by rememberSaveable { mutableStateOf("") }
    var amount by rememberSaveable { mutableStateOf("") }
    var type by rememberSaveable { mutableStateOf(TransactionType.EXPENSE) }
    var categoryId by rememberSaveable { mutableStateOf<String?>(null) }
    var date by rememberSaveable { mutableStateOf<LocalDate?>(LocalDate.now()) }
    var errors by remember { mutableStateOf(TransactionFormErrors()) }
    val availableCategories = categories.filter { it.type == type.toCategoryType() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Add transaction", style = MaterialTheme.typography.headlineSmall)
        }
        Spacer(Modifier.height(16.dp))

        Text("Type", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(8.dp))
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            val options = listOf(
                TransactionType.EXPENSE to "Expense",
                TransactionType.INCOME to "Income"
            )
            options.forEachIndexed { index, (option, label) ->
                SegmentedButton(
                    selected = type == option,
                    onClick = {
                        if (type != option) {
                            type = option
                            categoryId = null
                            errors = errors.copy(category = null)
                        }
                    },
                    shape = SegmentedButtonDefaults.itemShape(index, options.size),
                    label = { Text(label) }
                )
            }
        }
        Spacer(Modifier.height(16.dp))

        FormTextField(
            value = description,
            onValueChange = {
                description = it
                errors = errors.copy(description = null)
            },
            label = "Description",
            error = errors.description
        )
        Spacer(Modifier.height(4.dp))
        FormTextField(
            value = amount,
            onValueChange = {
                amount = it
                errors = errors.copy(amount = null)
            },
            label = "Amount",
            error = errors.amount,
            helper = "For example 125.50",
            keyboardType = KeyboardType.Decimal,
            imeAction = ImeAction.Done
        )
        Spacer(Modifier.height(4.dp))
        CategoryDropdown(
            categories = availableCategories,
            selectedId = categoryId,
            error = errors.category,
            onSelect = {
                categoryId = it
                errors = errors.copy(category = null)
            }
        )
        Spacer(Modifier.height(4.dp))
        DateField(
            date = date,
            error = errors.date,
            onDateSelected = {
                date = it
                errors = errors.copy(date = null)
            }
        )
        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                val result = validateTransactionForm(
                    TransactionFormInput(description, amount, type, categoryId, date)
                )
                when (result) {
                    is TransactionFormResult.Valid -> onSave(result.toTransaction())
                    is TransactionFormResult.Invalid -> errors = result.errors
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save transaction")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryDropdown(
    categories: List<Category>,
    selectedId: String?,
    error: String?,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = categories.firstOrNull { it.id == selectedId }?.name.orEmpty()

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        FormTextField(
            value = selectedName,
            onValueChange = {},
            label = "Category",
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            error = error,
            helper = "Choose where this transaction belongs",
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            categories.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category.name) },
                    onClick = {
                        onSelect(category.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateField(
    date: LocalDate?,
    error: String?,
    onDateSelected: (LocalDate) -> Unit
) {
    var showPicker by remember { mutableStateOf(false) }

    Box {
        FormTextField(
            value = date?.toString().orEmpty(),
            onValueChange = {},
            label = "Date",
            error = error,
            readOnly = true,
            trailingIcon = { Icon(Icons.Filled.CalendarMonth, contentDescription = null) }
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable(
                    role = Role.Button,
                    onClickLabel = "Select date",
                    onClick = { showPicker = true }
                )
        )
    }

    if (showPicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = date?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            onDateSelected(
                                Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                            )
                        }
                        showPicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

private fun TransactionType.toCategoryType(): CategoryType = when (this) {
    TransactionType.INCOME -> CategoryType.INCOME
    TransactionType.EXPENSE -> CategoryType.EXPENSE
}
