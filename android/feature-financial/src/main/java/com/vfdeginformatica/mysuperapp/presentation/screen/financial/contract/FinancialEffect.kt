package com.vfdeginformatica.mysuperapp.presentation.screen.financial.contract

sealed interface FinancialEffect {
    data class ShowToast(val message: String) : FinancialEffect

    /** [invoiceMonth] no formato yyyy-MM. */
    data class NavigateToCardInvoice(val cardId: String, val invoiceMonth: String) : FinancialEffect
}