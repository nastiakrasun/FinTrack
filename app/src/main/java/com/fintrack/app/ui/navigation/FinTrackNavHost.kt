package com.fintrack.app.ui.navigation

import android.util.Patterns
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.fintrack.app.data.repository.InMemoryDemoAuthRepository
import com.fintrack.app.domain.repository.AuthResult
import com.fintrack.app.ui.auth.LoginScreen
import com.fintrack.app.ui.auth.RegistrationScreen
import com.fintrack.app.ui.home.DashboardScreen

@Composable
fun FinTrackNavHost() {
    val navController = rememberNavController()
    val authRepository = remember { InMemoryDemoAuthRepository() }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var signedInEmail by remember { mutableStateOf("") }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val showNavigationBar = currentDestination?.route?.let { it in AppRoutes.main } == true

    Scaffold(
        bottomBar = {
            if (showNavigationBar) {
                NavigationBar {
                    mainNavigationItems.forEach { item ->
                        val selected = currentDestination?.hierarchy?.any {
                            it.route == item.route
                        } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(AppRoutes.HOME) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) }
                        )
                    }
                }
            }
        }
    ) { contentPadding ->
        NavHost(
            navController = navController,
            startDestination = AppRoutes.LOGIN,
            modifier = Modifier.padding(contentPadding)
        ) {
            composable(AppRoutes.LOGIN) {
                LoginScreen(
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
                                val result = authRepository.login(
                                    normalizedEmail,
                                    password.toCharArray()
                                )
                                password = ""
                                if (result == AuthResult.SUCCESS) {
                                    signedInEmail = normalizedEmail
                                    errorMessage = null
                                    navController.navigate(AppRoutes.HOME) {
                                        popUpTo(AppRoutes.LOGIN) { inclusive = true }
                                        launchSingleTop = true
                                    }
                                } else {
                                    errorMessage = "Email or password is incorrect."
                                }
                            }
                        }
                    },
                    onRegisterClick = {
                        errorMessage = null
                        password = ""
                        navController.navigate(AppRoutes.REGISTER)
                    }
                )
            }

            composable(AppRoutes.REGISTER) {
                RegistrationScreen(
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
                                val result = authRepository.register(
                                    normalizedEmail,
                                    password.toCharArray()
                                )
                                password = ""
                                confirmPassword = ""
                                when (result) {
                                    AuthResult.SUCCESS -> {
                                        signedInEmail = normalizedEmail
                                        errorMessage = null
                                        navController.navigate(AppRoutes.HOME) {
                                            popUpTo(AppRoutes.LOGIN) { inclusive = true }
                                            launchSingleTop = true
                                        }
                                    }

                                    AuthResult.EMAIL_ALREADY_REGISTERED ->
                                        errorMessage =
                                            "An account with this email already exists."

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
                        navController.popBackStack(
                            route = AppRoutes.REGISTER,
                            inclusive = true
                        )
                    }
                )
            }

            composable(AppRoutes.HOME) {
                DashboardScreen(
                    email = signedInEmail,
                    onLogout = {
                        signedInEmail = ""
                        email = ""
                        password = ""
                        confirmPassword = ""
                        errorMessage = null
                        navController.navigate(AppRoutes.LOGIN) {
                            popUpTo(AppRoutes.HOME) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(AppRoutes.TRANSACTIONS) {
                PlaceholderScreen(
                    title = "Transactions",
                    message = "Your transactions will appear here"
                )
            }

            composable(AppRoutes.BUDGET) {
                PlaceholderScreen(
                    title = "Budget",
                    message = "Budget management"
                )
            }

            composable(AppRoutes.STATISTICS) {
                PlaceholderScreen(
                    title = "Statistics",
                    message = "Financial statistics"
                )
            }
        }
    }
}

@Composable
private fun PlaceholderScreen(
    title: String,
    message: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        Text(
            text = message,
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

private data class MainNavigationItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

private val mainNavigationItems = listOf(
    MainNavigationItem(AppRoutes.HOME, "Home", Icons.Filled.Home),
    MainNavigationItem(AppRoutes.TRANSACTIONS, "Transactions", Icons.AutoMirrored.Filled.ReceiptLong),
    MainNavigationItem(AppRoutes.BUDGET, "Budget", Icons.Filled.AccountBalanceWallet),
    MainNavigationItem(AppRoutes.STATISTICS, "Statistics", Icons.Filled.BarChart)
)

private const val MIN_PASSWORD_LENGTH = 8
