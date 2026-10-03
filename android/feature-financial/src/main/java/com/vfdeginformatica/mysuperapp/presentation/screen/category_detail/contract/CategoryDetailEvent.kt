package com.vfdeginformatica.mysuperapp.presentation.screen.category_detail.contract

import com.vfdeginformatica.mysuperapp.presentation.screen.new_transaction.contract.TransactionType

sealed interface CategoryDetailEvent {
    data class TitleChanged(val title: String) : CategoryDetailEvent
    data class DescriptionChanged(val description: String) : CategoryDetailEvent
    data class TypeChanged(val type: TransactionType) : CategoryDetailEvent
    data object SaveClicked : CategoryDetailEvent
    data object DeleteClicked : CategoryDetailEvent
    data object DeleteConfirmed : CategoryDetailEvent
    data object DeleteDismissed : CategoryDetailEvent
}
