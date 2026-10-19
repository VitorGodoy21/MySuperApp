package com.vfdeginformatica.mysuperapp.presentation.screen.financial.contract

import com.vfdeginformatica.mysuperapp.domain.model.RecurrenceEditScope
import com.vfdeginformatica.mysuperapp.domain.model.Transaction

sealed interface FinancialEvent {
    data object OnPreviousMonth : FinancialEvent
    data object OnNextMonth : FinancialEvent
    data object OnRefresh : FinancialEvent
    data class OnTabChanged(val tab: FinancialTab) : FinancialEvent

    // Aba Mês: lista item a item recolhível
    data object OnToggleMonthList : FinancialEvent

    // Aba Mês: abre a fatura do cartão no mês selecionado
    data class OnCardInvoiceClicked(val cardId: String) : FinancialEvent

    // Aba Filtros
    data class OnTypeFilterChanged(val filter: TransactionTypeFilter) : FinancialEvent
    data class OnPaymentMethodFilterChanged(val filter: PaymentMethodFilter) : FinancialEvent
    data class OnCategoryFilterToggled(val categoryId: String) : FinancialEvent
    data object OnCategoryFilterCleared : FinancialEvent
    data class OnCardFilterChanged(val cardId: String?) : FinancialEvent
    data object OnRecurringOnlyToggled : FinancialEvent
    data object OnInstallmentsOnlyToggled : FinancialEvent
    data class OnGroupByChanged(val groupBy: FilterGroupBy) : FinancialEvent
    data object OnClearFilters : FinancialEvent

    // Edição/exclusão de transações (ex.: ajuste do salário real)
    data class OnTransactionClicked(val transaction: Transaction) : FinancialEvent
    data object OnEditDismiss : FinancialEvent
    data class OnEditConfirm(val title: String, val amount: Double) : FinancialEvent
    data object OnDeleteTransaction : FinancialEvent

    // Recorrentes: escolha do escopo ("só este mês" / "este e próximos")
    data class OnRecurringScopeConfirm(val scope: RecurrenceEditScope) : FinancialEvent
    data object OnRecurringScopeDismiss : FinancialEvent
}

/** Ação pendente sobre uma recorrente, aguardando o usuário escolher o escopo. */
sealed interface PendingRecurringAction {
    data class Update(val title: String, val amount: Double) : PendingRecurringAction
    data object Delete : PendingRecurringAction
}

enum class FinancialTab {
    MONTH, FILTERS
}

enum class TransactionTypeFilter {
    ALL, INCOME, EXPENSE
}

enum class PaymentMethodFilter {
    ALL, CREDIT, DEBIT
}

enum class FilterGroupBy {
    NONE, CATEGORY, CARD
}