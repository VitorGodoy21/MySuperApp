package com.vfdeginformatica.mysuperapp.presentation.screen.financial.contract

sealed interface FinancialEffect {
    data class ShowToast(val message: String) : FinancialEffect

    /** [invoiceMonth] no formato yyyy-MM. */
    data class NavigateToCardInvoice(val cardId: String, val invoiceMonth: String) : FinancialEffect

    /** [month] no formato yyyy-MM. */
    data class NavigateToCategoryExpenses(val month: String) : FinancialEffect

    /** [month] no formato yyyy-MM; abre o relatório mensal do mês selecionado. */
    data class NavigateToFinancialReport(val month: String) : FinancialEffect
}