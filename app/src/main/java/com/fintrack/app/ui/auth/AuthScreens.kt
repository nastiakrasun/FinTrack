package com.fintrack.app.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun LoginScreen(
    email: String,
    password: String,
    errorMessage: String?,
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
        errorMessage = errorMessage,
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
    errorMessage: String?,
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
        errorMessage = errorMessage,
        submitLabel = "Create account",
        onEmailChange = onEmailChange,
        onPasswordChange = onPasswordChange,
        onSubmit = onRegister,
        confirmPassword = confirmPassword,
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
    errorMessage: String?,
    submitLabel: String,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    confirmPassword: String? = null,
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

        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Email") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )
        if (confirmPassword != null && onConfirmPasswordChange != null) {
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = onConfirmPasswordChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Confirm password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )
        }

        if (errorMessage != null) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onSubmit,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(submitLabel)
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
