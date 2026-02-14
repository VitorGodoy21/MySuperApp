package com.vfdeginformatica.mysuperapp.presentation.screen.new_transaction.contract

import java.time.LocalDate

data class NewTransactionUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val transactionType: TransactionType = TransactionType.EXPENSE,
    val title: String = "",
    val amount: String = "",
    val description: String = "",
    val date: LocalDate = LocalDate.now(),
    val category: String = "",
    val paymentMethod: String = "",
    val isRecurring: Boolean = false,
    // Expense specific
    val cardId: String = "",
    val invoiceMonth: String = "",
)

enum class TransactionType {
    INCOME, EXPENSE
}