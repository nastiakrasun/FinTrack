package com.fintrack.app.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.fintrack.app.ui.components.FormTextField
import com.fintrack.app.ui.components.PasswordField
import com.fintrack.app.ui.form.MIN_PASSWORD_LENGTH

@Composable
fun LoginScreen(
    email: String,
    password: String,
    emailError: String?,
    passwordError: String?,
    formError: String?,
    isLoading: Boolean,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLogin: () -> Unit,
    onRegisterClick: () -> Unit
) {
    AuthForm(
        title = "Welcome back",
        subtitle = "Sign in to continue to FinTrack.",
        email = email,
        password = password,
        emailError = emailError,
        passwordError = passwordError,
        formError = formError,
        isLoading = isLoading,
        submitLabel = "Log in",
        onEmailChange = onEmailChange,
        onPasswordChange = onPasswordChange,
        onSubmit = onLogin,
        footer = {
            TextButton(onClick = onRegisterClick) {
                Text("New to FinTrack? Create an account")
            }
        }
    )
}

@Composable
fun RegistrationScreen(
    email: String,
    password: String,
    confirmPassword: String,
    emailError: String?,
    passwordError: String?,
    confirmPasswordError: String?,
    formError: String?,
    isLoading: Boolean,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onRegister: () -> Unit,
    onLoginClick: () -> Unit
) {
    AuthForm(
        title = "Create your account",
        subtitle = "Start keeping track of your finances.",
        email = email,
        password = password,
        emailError = emailError,
        passwordError = passwordError,
        formError = formError,
        isLoading = isLoading,
        submitLabel = "Create account",
        onEmailChange = onEmailChange,
        onPasswordChange = onPasswordChange,
        onSubmit = onRegister,
        confirmPassword = confirmPassword,
        confirmPasswordError = confirmPasswordError,
        onConfirmPasswordChange = onConfirmPasswordChange,
        footer = {
            TextButton(onClick = onLoginClick) {
                Text("Already have an account? Log in")
            }
        }
    )
}

@Composable
private fun AuthForm(
    title: String,
    subtitle: String,
    email: String,
    password: String,
    emailError: String?,
    passwordError: String?,
    formError: String?,
    isLoading: Boolean,
    submitLabel: String,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    confirmPassword: String? = null,
    confirmPasswordError: String? = null,
    onConfirmPasswordChange: ((String) -> Unit)? = null,
    footer: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 48.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "FinTrack",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(32.dp))
        Text(text = title, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(text = subtitle, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(24.dp))

        val isRegistration = confirmPassword != null && onConfirmPasswordChange != null

        FormTextField(
            value = email,
            onValueChange = onEmailChange,
            label = "Email",
            error = emailError,
            keyboardType = KeyboardType.Email
        )
        Spacer(Modifier.height(4.dp))
        PasswordField(
            value = password,
            onValueChange = onPasswordChange,
            label = "Password",
            error = passwordError,
            helper = if (isRegistration) "At least $MIN_PASSWORD_LENGTH characters." else null,
            imeAction = if (isRegistration) ImeAction.Next else ImeAction.Done
        )
        if (confirmPassword != null && onConfirmPasswordChange != null) {
            Spacer(Modifier.height(4.dp))
            PasswordField(
                value = confirmPassword,
                onValueChange = onConfirmPasswordChange,
                label = "Confirm password",
                error = confirmPasswordError,
                imeAction = ImeAction.Done
            )
        }

        if (formError != null) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = formError,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onSubmit,
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isLoading) "Please wait…" else submitLabel)
        }
        footer()
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Demo authentication only. Account data is kept in memory on this device.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
