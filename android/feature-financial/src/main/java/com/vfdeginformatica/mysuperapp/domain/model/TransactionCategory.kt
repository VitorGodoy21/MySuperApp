package com.vfdeginformatica.mysuperapp.domain.model

import com.vfdeginformatica.mysuperapp.presentation.screen.new_transaction.contract.TransactionType

data class TransactionCategory(
    val id: String,
    val title: String,
    val description: String,
    val type: TransactionType,
)
