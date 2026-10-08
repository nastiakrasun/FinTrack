package com.fintrack.app.ui

import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.fintrack.app.AppContainer
import com.fintrack.app.ui.auth.AuthViewModel
import com.fintrack.app.ui.home.HomeViewModel
import com.fintrack.app.ui.transactions.TransactionViewModel

// Plain factory instead of a DI framework: each ViewModel receives its repositories explicitly.
fun finTrackViewModelFactory(container: AppContainer) = viewModelFactory {
    initializer { AuthViewModel(container.authRepository) }
    initializer { HomeViewModel(container.financeRepository) }
    initializer { TransactionViewModel(container.financeRepository) }
}
