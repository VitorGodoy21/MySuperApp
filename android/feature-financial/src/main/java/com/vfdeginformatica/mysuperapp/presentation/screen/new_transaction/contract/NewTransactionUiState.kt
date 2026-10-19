package com.vfdeginformatica.mysuperapp.presentation.screen.new_transaction.contract

import com.vfdeginformatica.mysuperapp.data.remote.dto.CardDto
import com.vfdeginformatica.mysuperapp.data.remote.dto.CategoryTransactionDto
import com.vfdeginformatica.mysuperapp.presentation.common.CurrencyVisualTransformation
import java.time.LocalDate

data class NewTransactionUiState(
    val isLoading: Boolean = false,
    val isLoadingCards: Boolean = false,
    val isLoadingCategories: Boolean = false,
    val errorMessage: String? = null,
    val transactionType: TransactionType = TransactionType.EXPENSE,
    val title: String = "",
    // Valor em centavos, apenas dígitos (ex.: "12480" = R$ 124,80)
    val amount: String = "",
    val description: String = "",
    val date: LocalDate = LocalDate.now(),
    val showDatePicker: Boolean = false,
    val selectedCategories: List<CategoryTransactionDto> = emptyList(),
    val availableCategories: List<CategoryTransactionDto>? = null,
    val paymentMethod: String = "",
    val isRecurring: Boolean = false,
    // Expense specific
    val cardId: String = "",
    val invoiceMonth: String = "",
    val installments: String = "1",
    val availableCards: List<CardDto>? = null,
    val isSubmitEnabled: Boolean = false
) {
    /** Todos os cartões são elegíveis como destino/origem, para receita e despesa. */
    val selectableCards: List<CardDto>
        get() = availableCards.orEmpty()

    /** Categorias compatíveis com o tipo de transação selecionado (despesa ou receita). */
    val categoriesForType: List<CategoryTransactionDto>
        get() = availableCategories.orEmpty().filter { it.transactionType == transactionType.name }

    /** Valor em reais (Double, formato aceito pelo Firestore). */
    val amountValue: Double
        get() = CurrencyVisualTransformation.centsToDouble(amount)
}

enum class TransactionType {
    INCOME, EXPENSE
}