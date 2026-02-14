package com.vfdeginformatica.mysuperapp.data.remote.dto

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import java.math.BigDecimal

data class TransactionDto(
    @DocumentId var id: String? = null,
    val title: String? = null,
    val description: String? = null,
    val transactionType: String? = null,
    val paymentMethod: String,
    val amount: BigDecimal,
    val isRecurring: Boolean,
    val transactionDate: Timestamp? = null,
    val category: List<String>? = null,
    val status: String? = null,
    val cardId: String? = null,
    val invoiceMonth: String? = null,
    val installmentGroupId: String? = null,
)
