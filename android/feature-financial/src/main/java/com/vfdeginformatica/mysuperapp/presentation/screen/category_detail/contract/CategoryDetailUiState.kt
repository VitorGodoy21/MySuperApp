package com.vfdeginformatica.mysuperapp.presentation.screen.category_detail.contract

import com.vfdeginformatica.mysuperapp.presentation.screen.new_transaction.contract.TransactionType

data class CategoryDetailUiState(
    val categoryId: String? = null,
    val isLoading: Boolean = false,
    val title: String = "",
    val description: String = "",
    val transactionType: TransactionType = TransactionType.EXPENSE,
    val isSaving: Boolean = false,
    val isDeleting: Boolean = false,
    val showDeleteConfirmation: Boolean = false,
    val isSubmitEnabled: Boolean = false,
) {
    val isEditing: Boolean
        get() = !categoryId.isNullOrBlank()

    val isBusy: Boolean
        get() = isSaving || isDeleting
}
