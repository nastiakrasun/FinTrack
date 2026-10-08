package com.fintrack.app.ui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.fintrack.app.ui.auth.AuthViewModel
import com.fintrack.app.ui.auth.LoginScreen
import com.fintrack.app.ui.auth.RegistrationScreen
import com.fintrack.app.ui.home.DashboardScreen
import com.fintrack.app.ui.home.HomeViewModel
import com.fintrack.app.ui.transactions.AddTransactionScreen
import com.fintrack.app.ui.transactions.TransactionViewModel
import com.fintrack.app.ui.transactions.TransactionsScreen

@Composable
fun FinTrackNavHost(viewModelFactory: ViewModelProvider.Factory) {
    val navController = rememberNavController()
    // Activity-scoped: login and registration share one form state that survives rotation.
    val authViewModel: AuthViewModel = viewModel(factory = viewModelFactory)
    val authState by authViewModel.uiState.collectAsStateWithLifecycle()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val currentRoute = currentDestination?.route
    val showNavigationBar = currentRoute?.let { it in AppRoutes.main } == true

    // Navigation stays in the UI: the ViewModel only exposes whether the user is signed in.
    LaunchedEffect(authState.signedInEmail, currentRoute) {
        val isSignedIn = authState.signedInEmail != null
        when {
            isSignedIn && currentRoute in AppRoutes.unauthenticated ->
                navController.navigate(AppRoutes.HOME) {
                    popUpTo(AppRoutes.LOGIN) { inclusive = true }
                    launchSingleTop = true
                }

            !isSignedIn && currentRoute in AppRoutes.authenticated ->
                navController.navigate(AppRoutes.LOGIN) {
                    popUpTo(navController.graph.id) { inclusive = true }
                    launchSingleTop = true
                }
        }
    }

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
            modifier = Modifier
                .padding(contentPadding)
                .consumeWindowInsets(contentPadding)
        ) {
            composable(AppRoutes.LOGIN) {
                LoginScreen(
                    email = authState.email,
                    password = authState.password,
                    emailError = authState.emailError,
                    passwordError = authState.passwordError,
                    formError = authState.formError,
                    isLoading = authState.isLoading,
                    onEmailChange = authViewModel::onEmailChange,
                    onPasswordChange = authViewModel::onPasswordChange,
                    onLogin = authViewModel::login,
                    onRegisterClick = {
                        authViewModel.onAuthScreenSwitched()
                        navController.navigate(AppRoutes.REGISTER)
                    }
                )
            }

            composable(AppRoutes.REGISTER) {
                RegistrationScreen(
                    email = authState.email,
                    password = authState.password,
                    confirmPassword = authState.confirmPassword,
                    emailError = authState.emailError,
                    passwordError = authState.passwordError,
                    confirmPasswordError = authState.confirmPasswordError,
                    formError = authState.formError,
                    isLoading = authState.isLoading,
                    onEmailChange = authViewModel::onEmailChange,
                    onPasswordChange = authViewModel::onPasswordChange,
                    onConfirmPasswordChange = authViewModel::onConfirmPasswordChange,
                    onRegister = authViewModel::register,
                    onLoginClick = {
                        authViewModel.onAuthScreenSwitched()
                        navController.popBackStack(
                            route = AppRoutes.REGISTER,
                            inclusive = true
                        )
                    }
                )
            }

            composable(AppRoutes.HOME) {
                val homeViewModel: HomeViewModel = viewModel(factory = viewModelFactory)
                val homeState by homeViewModel.uiState.collectAsStateWithLifecycle()
                DashboardScreen(
                    email = authState.signedInEmail.orEmpty(),
                    state = homeState,
                    onLogout = authViewModel::logout
                )
            }

            composable(AppRoutes.TRANSACTIONS) {
                val transactionViewModel: TransactionViewModel = viewModel(factory = viewModelFactory)
                val state by transactionViewModel.uiState.collectAsStateWithLifecycle()
                TransactionsScreen(
                    transactions = state.transactions,
                    categories = state.categories,
                    onAddTransaction = { navController.navigate(AppRoutes.ADD_TRANSACTION) }
                )
            }

            composable(AppRoutes.ADD_TRANSACTION) {
                // Scoped to this destination, so an unsaved form is discarded when it is closed.
                val transactionViewModel: TransactionViewModel = viewModel(factory = viewModelFactory)
                val state by transactionViewModel.uiState.collectAsStateWithLifecycle()
                val isSaved = state.form.isSaved
                LaunchedEffect(isSaved) {
                    if (isSaved) navController.popBackStack()
                }
                AddTransactionScreen(
                    form = state.form,
                    availableCategories = state.availableCategories,
                    onDescriptionChange = transactionViewModel::onDescriptionChange,
                    onAmountChange = transactionViewModel::onAmountChange,
                    onTypeChange = transactionViewModel::onTypeChange,
                    onCategoryChange = transactionViewModel::onCategoryChange,
                    onDateChange = transactionViewModel::onDateChange,
                    onSave = transactionViewModel::saveTransaction,
                    onBack = { navController.popBackStack() }
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
