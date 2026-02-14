package com.vfdeginformatica.mysuperapp.presentation.screen.new_transaction.contract

sealed interface NewTransactionEffect {
    data class ShowToast(val message: String) : NewTransactionEffect
    data object TransactionSaved : NewTransactionEffect
}