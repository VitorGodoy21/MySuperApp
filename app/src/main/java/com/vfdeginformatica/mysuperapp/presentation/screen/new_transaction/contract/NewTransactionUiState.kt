package com.vfdeginformatica.mysuperapp.presentation.screen.new_transaction.contract

import com.vfdeginformatica.mysuperapp.data.remote.dto.CardDto
import java.time.LocalDate

data class NewTransactionUiState(
    val isLoading: Boolean = false,
    val isLoadingCards: Boolean = false,
    val errorMessage: String? = null,
    val transactionType: TransactionType = TransactionType.EXPENSE,
    val title: String = "",
    val amount: String = "",
    val description: String = "",
    val date: LocalDate = LocalDate.now(),
    val selectedCategories: List<String> = emptyList(),
    val availableCategories: List<String> = listOf(
        "Alimentação",
        "Transporte",
        "Lazer",
        "Saúde",
        "Educação",
        "Moradia",
        "Outros"
    ),
    val paymentMethod: String = "",
    val isRecurring: Boolean = false,
    // Expense specific
    val cardId: String = "",
    val invoiceMonth: String = "",
    val availableCards: List<CardDto>? = null
)

enum class TransactionType {
    INCOME, EXPENSE
}