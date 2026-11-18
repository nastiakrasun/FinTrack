package com.fintrack.app.ui.api

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fintrack.app.domain.repository.DemoApiRepository
import com.fintrack.app.domain.repository.DemoApiResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ApiDemoUiState(
    val isLoading: Boolean = false,
    val authenticated: Boolean? = null,
    val message: String? = null,
    val errorMessage: String? = null
)

class ApiDemoViewModel(
    private val repository: DemoApiRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(ApiDemoUiState())
    val uiState: StateFlow<ApiDemoUiState> = _uiState.asStateFlow()

    fun checkApi() {
        if (_uiState.value.isLoading) return
        _uiState.value = ApiDemoUiState(isLoading = true)
        viewModelScope.launch {
            when (val result = repository.checkProtectedEndpoint()) {
                is DemoApiResult.Success -> _uiState.value = ApiDemoUiState(
                    authenticated = result.authenticated,
                    message = "Protected API responded successfully. Bearer token was accepted."
                )
                is DemoApiResult.Failure -> _uiState.value = ApiDemoUiState(errorMessage = result.message)
            }
        }
    }
}
