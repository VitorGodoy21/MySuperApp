package com.vfdeginformatica.mysuperapp.presentation.screen.card_invoice.contract

sealed interface CardInvoiceEffect {
    data class ShowToast(val message: String) : CardInvoiceEffect
}
