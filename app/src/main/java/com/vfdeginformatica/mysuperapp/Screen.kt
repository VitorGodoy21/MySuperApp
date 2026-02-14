package com.vfdeginformatica.mysuperapp

sealed class Screen(val route: String) {
    object SplashScreen : Screen("splash_screen")
    object HomeScreen : Screen("home_screen")
    object LoginScreen : Screen("login_screen")
    object FinancialScreen : Screen("financial_screen")
    object NewTransactionScreen : Screen("new_transaction_screen")
}