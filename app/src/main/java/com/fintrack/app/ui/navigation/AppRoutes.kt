package com.fintrack.app.ui.navigation

object AppRoutes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val HOME = "home"
    const val TRANSACTIONS = "transactions"
    const val BUDGET = "budget"
    const val STATISTICS = "statistics"
    const val ADD_TRANSACTION = "add_transaction"

    val main = listOf(HOME, TRANSACTIONS, BUDGET, STATISTICS)
    val unauthenticated = listOf(LOGIN, REGISTER)
    val authenticated = main + ADD_TRANSACTION
}
