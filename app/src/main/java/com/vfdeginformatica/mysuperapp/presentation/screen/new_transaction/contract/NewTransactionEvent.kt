package com.vfdeginformatica.mysuperapp.presentation.screen.new_transaction.contract

import java.time.LocalDate

sealed interface NewTransactionEvent {
    data class TypeChanged(val type: TransactionType) : NewTransactionEvent
    data class TitleChanged(val title: String) : NewTransactionEvent
    data class AmountChanged(val amount: String) : NewTransactionEvent
    data class DescriptionChanged(val description: String) : NewTransactionEvent
    data class DateChanged(val date: LocalDate) : NewTransactionEvent
    data class CategoryToggled(val category: String) : NewTransactionEvent
    data class PaymentMethodChanged(val method: String) : NewTransactionEvent
    data class RecurringChanged(val isRecurring: Boolean) : NewTransactionEvent
    data class CardIdChanged(val cardId: String) : NewTransactionEvent
    data class InvoiceMonthChanged(val month: String) : NewTransactionEvent
    data object SaveClicked : NewTransactionEvent
}