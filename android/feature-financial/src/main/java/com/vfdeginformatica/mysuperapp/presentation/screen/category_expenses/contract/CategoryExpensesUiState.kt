package com.vfdeginformatica.mysuperapp.presentation.screen.category_expenses.contract

import java.time.YearMonth

data class CategoryExpensesUiState(
    val month: YearMonth = YearMonth.now(),
    val isLoadingTransactions: Boolean = true,
    val isLoadingCategories: Boolean = true,
    val report: CategoryExpenseReport = CategoryExpenseReport(),
) {
    val isLoading: Boolean
        get() = isLoadingTransactions || isLoadingCategories
}
