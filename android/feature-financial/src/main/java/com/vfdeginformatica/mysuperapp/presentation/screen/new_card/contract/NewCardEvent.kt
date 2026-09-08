package com.vfdeginformatica.mysuperapp.presentation.screen.new_card.contract

sealed interface NewCardEvent {
    data class BankChanged(val bank: String) : NewCardEvent
    data class MethodToggled(val method: String) : NewCardEvent
    data class FinalCodeChanged(val finalCode: String) : NewCardEvent
    data class ClosingDayChanged(val day: String) : NewCardEvent
    data class InvoiceDueDateChanged(val day: String) : NewCardEvent
    data class InitialBalanceChanged(val amount: String) : NewCardEvent
    data class CurrentInvoiceChanged(val amount: String) : NewCardEvent
    data object SaveClicked : NewCardEvent
}
