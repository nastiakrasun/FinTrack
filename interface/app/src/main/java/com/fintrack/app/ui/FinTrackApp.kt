package com.fintrack.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.fintrack.app.FinTrackApplication
import com.fintrack.app.ui.navigation.FinTrackNavHost

@Composable
fun FinTrackApp() {
    val application = LocalContext.current.applicationContext as FinTrackApplication
    val viewModelFactory = remember(application) { finTrackViewModelFactory(application.container) }
    FinTrackNavHost(viewModelFactory)
}
