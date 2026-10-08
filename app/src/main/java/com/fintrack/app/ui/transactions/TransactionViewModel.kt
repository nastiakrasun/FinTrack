package com.fintrack.app.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fintrack.app.domain.model.Category
import com.fintrack.app.domain.model.CategoryType
import com.fintrack.app.domain.model.Transaction
import com.fintrack.app.domain.model.TransactionType
import com.fintrack.app.domain.model.newestFirst
import com.fintrack.app.domain.repository.FinanceRepository
import com.fintrack.app.ui.form.TransactionFormErrors
import com.fintrack.app.ui.form.TransactionFormInput
import com.fintrack.app.ui.form.TransactionFormResult
import com.fintrack.app.ui.form.toTransaction
import com.fintrack.app.ui.form.validateTransactionForm
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID

data class TransactionFormState(
    val description: String = "",
    val amount: String = "",
    val type: TransactionType = TransactionType.EXPENSE,
    val categoryId: String? = null,
    val date: LocalDate? = null,
    val errors: TransactionFormErrors = TransactionFormErrors(),
    val isSaving: Boolean = false,
    val isSaved: Boolean = false
)

data class TransactionUiState(
    val transactions: List<Transaction> = emptyList(),
    val categories: List<Category> = emptyList(),
    val form: TransactionFormState = TransactionFormState()
) {
    // Only categories matching the selected transaction type can be chosen.
    val availableCategories: List<Category>
        get() = categories.filter { it.type == form.type.toCategoryType() }
}

class TransactionViewModel(
    private val financeRepository: FinanceRepository,
    private val today: () -> LocalDate = { LocalDate.now() },
    private val newId: () -> String = { UUID.randomUUID().toString() }
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        TransactionUiState(form = TransactionFormState(date = today()))
    )
    val uiState: StateFlow<TransactionUiState> = _uiState.asStateFlow()

    init {
        financeRepository.observeTransactions()
            .onEach { transactions ->
                _uiState.update { it.copy(transactions = transactions.newestFirst()) }
            }
            .launchIn(viewModelScope)
        financeRepository.observeCategories()
            .onEach { categories -> _uiState.update { it.copy(categories = categories) } }
            .launchIn(viewModelScope)
    }

    fun onDescriptionChange(value: String) = updateForm {
        it.copy(description = value, errors = it.errors.copy(description = null))
    }

    fun onAmountChange(value: String) = updateForm {
        it.copy(amount = value, errors = it.errors.copy(amount = null))
    }

    fun onTypeChange(type: TransactionType) = updateForm {
        if (it.type == type) it else it.copy(type = type, categoryId = null, errors = it.errors.copy(category = null))
    }

    fun onCategoryChange(categoryId: String) = updateForm {
        it.copy(categoryId = categoryId, errors = it.errors.copy(category = null))
    }

    fun onDateChange(date: LocalDate) = updateForm {
        it.copy(date = date, errors = it.errors.copy(date = null))
    }

    fun saveTransaction() {
        val form = _uiState.value.form
        if (form.isSaved || form.isSaving) return

        val result = validateTransactionForm(
            TransactionFormInput(
                description = form.description,
                amount = form.amount,
                type = form.type,
                categoryId = form.categoryId,
                date = form.date
            )
        )
        when (result) {
            is TransactionFormResult.Valid -> {
                updateForm { it.copy(errors = TransactionFormErrors(), isSaving = true) }
                viewModelScope.launch {
                    financeRepository.addTransaction(result.toTransaction(newId()))
                    updateForm { it.copy(isSaving = false, isSaved = true) }
                }
            }

            is TransactionFormResult.Invalid -> updateForm { it.copy(errors = result.errors) }
        }
    }

    private fun updateForm(transform: (TransactionFormState) -> TransactionFormState) {
        _uiState.update { it.copy(form = transform(it.form)) }
    }
}

private fun TransactionType.toCategoryType(): CategoryType = when (this) {
    TransactionType.INCOME -> CategoryType.INCOME
    TransactionType.EXPENSE -> CategoryType.EXPENSE
}
