package com.fintrack.app.ui.navigation

object AppRoutes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val HOME = "home"
    const val TRANSACTIONS = "transactions"
    const val BUDGET = "budget"
    const val STATISTICS = "statistics"

    val main = listOf(HOME, TRANSACTIONS, BUDGET, STATISTICS)
}
