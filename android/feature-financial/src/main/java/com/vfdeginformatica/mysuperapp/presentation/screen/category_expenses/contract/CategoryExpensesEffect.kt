package com.vfdeginformatica.mysuperapp.presentation.screen.category_expenses.contract

sealed interface CategoryExpensesEffect {
    data class ShowToast(val message: String) : CategoryExpensesEffect
}
