package com.vfdeginformatica.mysuperapp.presentation.screen.new_transaction.contract

import com.vfdeginformatica.mysuperapp.data.remote.dto.CardDto
import com.vfdeginformatica.mysuperapp.data.remote.dto.CategoryTransactionDto
import java.time.LocalDate

data class NewTransactionUiState(
    val isLoading: Boolean = false,
    val isLoadingCards: Boolean = false,
    val isLoadingCategories: Boolean = false,
    val errorMessage: String? = null,
    val transactionType: TransactionType = TransactionType.EXPENSE,
    val title: String = "",
    val amount: String = "",
    val description: String = "",
    val date: LocalDate = LocalDate.now(),
    val selectedCategories: List<CategoryTransactionDto> = emptyList(),
    val availableCategories: List<CategoryTransactionDto>? = null,
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