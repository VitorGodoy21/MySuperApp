package com.vfdeginformatica.mysuperapp.presentation.screen.new_card.contract

data class NewCardUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val bank: String = "",
    // Métodos selecionados: "CREDIT" e/ou "DEBIT"
    val paymentMethods: Set<String> = emptySet(),
    val finalCode: String = "",
    val closingDay: String = "",
    val invoiceDueDate: String = "",
    // Valores em centavos (apenas dígitos), exibidos pela máscara de moeda
    val initialBalance: String = "",
    val currentInvoice: String = "",
    val isSubmitEnabled: Boolean = false
) {
    val hasCredit: Boolean
        get() = paymentMethods.contains("CREDIT")
}
