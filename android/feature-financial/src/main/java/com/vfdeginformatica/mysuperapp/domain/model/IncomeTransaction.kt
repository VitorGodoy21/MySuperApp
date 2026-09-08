package com.vfdeginformatica.mysuperapp.domain.model

import com.google.firebase.Timestamp
import com.vfdeginformatica.mysuperapp.presentation.screen.new_transaction.contract.TransactionType

data class IncomeTransaction(
    val title: String? = "",
    val description: String? = "",
    val amount: Double? = null,
    val transactionDate: Timestamp? = null,
    val transactionType: TransactionType = TransactionType.INCOME,
    val cardId: String? = ""
)
