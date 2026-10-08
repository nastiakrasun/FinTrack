package com.fintrack.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fintrack.app.domain.repository.AuthRepository
import com.fintrack.app.domain.repository.AuthResult
import com.fintrack.app.ui.form.validateConfirmPassword
import com.fintrack.app.ui.form.validateEmail
import com.fintrack.app.ui.form.validatePassword
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
    val formError: String? = null,
    val isLoading: Boolean = false,
    // Non-null means the user is authenticated; the UI reacts to it by navigating.
    val signedInEmail: String? = null
)

class AuthViewModel(
    private val authRepository: AuthRepository,
    // Password hashing is CPU-heavy, so repository calls run off the main thread.
    private val workDispatcher: CoroutineDispatcher = Dispatchers.Default
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value, emailError = null, formError = null) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update {
            it.copy(
                password = value,
                passwordError = null,
                confirmPasswordError = null,
                formError = null
            )
        }
    }

    fun onConfirmPasswordChange(value: String) {
        _uiState.update {
            it.copy(confirmPassword = value, confirmPasswordError = null, formError = null)
        }
    }

    // Called when switching between Login and Register: keeps the email, drops secrets and errors.
    fun onAuthScreenSwitched() {
        _uiState.update {
            AuthUiState(email = it.email, signedInEmail = it.signedInEmail)
        }
    }

    fun login() {
        val current = _uiState.value
        if (current.isLoading) return

        val email = current.email.trim()
        val emailError = validateEmail(email)
        val passwordError = validatePassword(current.password)
        if (emailError != null || passwordError != null) {
            _uiState.update {
                it.copy(emailError = emailError, passwordError = passwordError, formError = null)
            }
            return
        }

        val password = current.password.toCharArray()
        _uiState.update {
            it.copy(
                password = "",
                emailError = null,
                passwordError = null,
                formError = null,
                isLoading = true
            )
        }
        viewModelScope.launch {
            val result = withContext(workDispatcher) { authRepository.login(email, password) }
            _uiState.update {
                if (result == AuthResult.SUCCESS) {
                    it.copy(isLoading = false, signedInEmail = email)
                } else {
                    it.copy(isLoading = false, formError = "Email or password is incorrect.")
                }
            }
        }
    }

    fun register() {
        val current = _uiState.value
        if (current.isLoading) return

        val email = current.email.trim()
        val emailError = validateEmail(email)
        val passwordError = validatePassword(current.password)
        val confirmPasswordError = validateConfirmPassword(current.password, current.confirmPassword)
        if (emailError != null || passwordError != null || confirmPasswordError != null) {
            _uiState.update {
                it.copy(
                    emailError = emailError,
                    passwordError = passwordError,
                    confirmPasswordError = confirmPasswordError,
                    formError = null
                )
            }
            return
        }

        val password = current.password.toCharArray()
        _uiState.update {
            it.copy(
                password = "",
                confirmPassword = "",
                emailError = null,
                passwordError = null,
                confirmPasswordError = null,
                formError = null,
                isLoading = true
            )
        }
        viewModelScope.launch {
            val result = withContext(workDispatcher) { authRepository.register(email, password) }
            _uiState.update {
                when (result) {
                    AuthResult.SUCCESS -> it.copy(isLoading = false, signedInEmail = email)
                    AuthResult.EMAIL_ALREADY_REGISTERED -> it.copy(
                        isLoading = false,
                        emailError = "An account with this email already exists."
                    )

                    AuthResult.INVALID_CREDENTIALS -> it.copy(
                        isLoading = false,
                        formError = "Registration failed. Please try again."
                    )
                }
            }
        }
    }

    fun logout() {
        _uiState.value = AuthUiState()
    }
}
