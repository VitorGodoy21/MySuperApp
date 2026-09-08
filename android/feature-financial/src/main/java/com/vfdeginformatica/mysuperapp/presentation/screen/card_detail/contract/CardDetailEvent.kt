package com.vfdeginformatica.mysuperapp.presentation.screen.card_detail.contract

sealed interface CardDetailEvent {
    data class BankChanged(val bank: String) : CardDetailEvent
    data class MethodToggled(val method: String) : CardDetailEvent
    data class FinalCodeChanged(val finalCode: String) : CardDetailEvent
    data class ClosingDayChanged(val day: String) : CardDetailEvent
    data class InvoiceDueDateChanged(val day: String) : CardDetailEvent
    data class BalanceToAddChanged(val amount: String) : CardDetailEvent
    data object SaveClicked : CardDetailEvent
    data object AddBalanceClicked : CardDetailEvent
}
