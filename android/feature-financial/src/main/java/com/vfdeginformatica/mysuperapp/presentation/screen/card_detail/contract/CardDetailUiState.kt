package com.vfdeginformatica.mysuperapp.presentation.screen.card_detail.contract

data class CardDetailUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isAddingBalance: Boolean = false,
    val cardId: String = "",
    val bank: String = "",
    // Métodos selecionados: "CREDIT" e/ou "DEBIT"
    val paymentMethods: Set<String> = emptySet(),
    val finalCode: String = "",
    val closingDay: String = "",
    val invoiceDueDate: String = "",
    // Valor em centavos (apenas dígitos), exibido pela máscara de moeda
    val balanceToAdd: String = "",
    val isSubmitEnabled: Boolean = false,
    val errorMessage: String? = null,
) {
    val hasCredit: Boolean
        get() = paymentMethods.contains("CREDIT")

    val isAddBalanceEnabled: Boolean
        get() = balanceToAdd.isNotBlank() && !isAddingBalance
}
