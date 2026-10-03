package com.vfdeginformatica.mysuperapp.presentation.screen.category_list.contract

import com.vfdeginformatica.mysuperapp.domain.model.TransactionCategory
import com.vfdeginformatica.mysuperapp.presentation.screen.new_transaction.contract.TransactionType

data class CategoryListUiState(
    val isLoading: Boolean = false,
    val categories: List<TransactionCategory> = emptyList(),
    val errorMessage: String? = null,
) {
    val expenseCategories: List<TransactionCategory>
        get() = categories.filter { it.type == TransactionType.EXPENSE }

    val incomeCategories: List<TransactionCategory>
        get() = categories.filter { it.type == TransactionType.INCOME }
}
