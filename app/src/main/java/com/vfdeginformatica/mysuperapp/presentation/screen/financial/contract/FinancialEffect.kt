package com.vfdeginformatica.mysuperapp.presentation.screen.financial.contract

sealed interface FinancialEffect {
    data class ShowToast(val message: String) : FinancialEffect
}