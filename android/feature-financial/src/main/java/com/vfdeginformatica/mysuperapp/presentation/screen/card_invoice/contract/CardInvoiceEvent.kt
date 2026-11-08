package com.vfdeginformatica.mysuperapp.presentation.screen.card_invoice.contract

import com.vfdeginformatica.mysuperapp.domain.model.RecurrenceEditScope
import com.vfdeginformatica.mysuperapp.domain.model.Transaction
import java.time.LocalDate

sealed interface CardInvoiceEvent {
    data object OnRefresh : CardInvoiceEvent

    // Pagamento da fatura
    data object OnPayClicked : CardInvoiceEvent
    data class OnPayConfirm(val amount: Double, val date: LocalDate) : CardInvoiceEvent
    data object OnPayDismiss : CardInvoiceEvent
    /** Abre o pagamento já registrado no dialog de edição (ajustar valor / excluir = reabrir fatura). */
    data object OnPaymentClicked : CardInvoiceEvent

    // Edição/exclusão de lançamentos
    data class OnTransactionClicked(val transaction: Transaction) : CardInvoiceEvent
    data object OnEditDismiss : CardInvoiceEvent
    data class OnEditConfirm(
        val title: String,
        val amount: Double,
        val categoryIds: List<String>
    ) : CardInvoiceEvent
    data object OnDeleteTransaction : CardInvoiceEvent

    // Recorrentes: escolha do escopo ("só este mês" / "este e próximos")
    data class OnRecurringScopeConfirm(val scope: RecurrenceEditScope) : CardInvoiceEvent
    data object OnRecurringScopeDismiss : CardInvoiceEvent
}
