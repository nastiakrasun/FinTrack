package com.fintrack.app.ui

import android.util.Patterns
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.fintrack.app.data.repository.InMemoryDemoAuthRepository
import com.fintrack.app.domain.repository.AuthResult
import com.fintrack.app.ui.auth.LoginScreen
import com.fintrack.app.ui.auth.RegistrationScreen
import com.fintrack.app.ui.home.DashboardScreen

private enum class AppScreen {
    LOGIN,
    REGISTRATION,
    DASHBOARD
}

@Composable
fun FinTrackApp() {
    val authRepository = remember { InMemoryDemoAuthRepository() }
    var screen by remember { mutableStateOf(AppScreen.LOGIN) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var signedInEmail by remember { mutableStateOf("") }

    when (screen) {
        AppScreen.LOGIN -> LoginScreen(
            email = email,
            password = password,
            errorMessage = errorMessage,
            onEmailChange = {
                email = it
                errorMessage = null
            },
            onPasswordChange = {
                password = it
                errorMessage = null
            },
            onLogin = {
                val normalizedEmail = email.trim()
                when {
                    !Patterns.EMAIL_ADDRESS.matcher(normalizedEmail).matches() ->
                        errorMessage = "Enter a valid email address."

                    password.isBlank() ->
                        errorMessage = "Enter your password."

                    else -> {
                        val result = authRepository.login(normalizedEmail, password.toCharArray())
                        password = ""
                        if (result == AuthResult.SUCCESS) {
                            signedInEmail = normalizedEmail
                            errorMessage = null
                            screen = AppScreen.DASHBOARD
                        } else {
                            errorMessage = "Email or password is incorrect."
                        }
                    }
                }
            },
            onRegisterClick = {
                errorMessage = null
                password = ""
                screen = AppScreen.REGISTRATION
            }
        )

        AppScreen.REGISTRATION -> RegistrationScreen(
            email = email,
            password = password,
            confirmPassword = confirmPassword,
            errorMessage = errorMessage,
            onEmailChange = {
                email = it
                errorMessage = null
            },
            onPasswordChange = {
                password = it
                errorMessage = null
            },
            onConfirmPasswordChange = {
                confirmPassword = it
                errorMessage = null
            },
            onRegister = {
                val normalizedEmail = email.trim()
                when {
                    !Patterns.EMAIL_ADDRESS.matcher(normalizedEmail).matches() ->
                        errorMessage = "Enter a valid email address."

                    password.length < MIN_PASSWORD_LENGTH ->
                        errorMessage = "Password must be at least 8 characters."

                    password != confirmPassword ->
                        errorMessage = "Passwords do not match."

                    else -> {
                        val result = authRepository.register(normalizedEmail, password.toCharArray())
                        password = ""
                        confirmPassword = ""
                        when (result) {
                            AuthResult.SUCCESS -> {
                                signedInEmail = normalizedEmail
                                errorMessage = null
                                screen = AppScreen.DASHBOARD
                            }

                            AuthResult.EMAIL_ALREADY_REGISTERED ->
                                errorMessage = "An account with this email already exists."

                            AuthResult.INVALID_CREDENTIALS ->
                                errorMessage = "Registration failed. Please try again."
                        }
                    }
                }
            },
            onLoginClick = {
                password = ""
                confirmPassword = ""
                errorMessage = null
                screen = AppScreen.LOGIN
            }
        )

        AppScreen.DASHBOARD -> DashboardScreen(
            email = signedInEmail,
            onLogout = {
                signedInEmail = ""
                email = ""
                password = ""
                confirmPassword = ""
                errorMessage = null
                screen = AppScreen.LOGIN
            }
        )
    }
}

private const val MIN_PASSWORD_LENGTH = 8
