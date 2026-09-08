package com.vfdeginformatica.mysuperapp.presentation.screen.financial.contract

import com.vfdeginformatica.mysuperapp.domain.model.Transaction

sealed interface FinancialEvent {
    data object OnPreviousMonth : FinancialEvent
    data object OnNextMonth : FinancialEvent
    data object OnRefresh : FinancialEvent
    data class OnTypeFilterChanged(val filter: TransactionTypeFilter) : FinancialEvent
    data class OnCategoryFilterChanged(val categoryId: String?) : FinancialEvent
    data class OnCardFilterChanged(val cardId: String?) : FinancialEvent
    data object OnRecurringExpensesFilterToggled : FinancialEvent

    // Aba Projeção
    data class OnTabChanged(val tab: FinancialTab) : FinancialEvent
    data object OnProjectionPreviousMonth : FinancialEvent
    data object OnProjectionNextMonth : FinancialEvent

    // Edição/exclusão de transações (ex.: ajuste do salário real)
    data class OnTransactionClicked(val transaction: Transaction) : FinancialEvent
    data object OnEditDismiss : FinancialEvent
    data class OnEditConfirm(val title: String, val amount: Double) : FinancialEvent
    data object OnDeleteTransaction : FinancialEvent
}

enum class FinancialTab {
    MONTH, PROJECTION
}

enum class TransactionTypeFilter {
    ALL, INCOME, EXPENSE
}