package com.vfdeginformatica.mysuperapp.data.remote.dto

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class CategoryTransactionDto(
    @DocumentId
    var id: String = "",
    var title: String = "",
    var description: String = "",
    // EXPENSE | INCOME — documentos antigos sem o campo são tratados como EXPENSE
    var transactionType: String = TRANSACTION_TYPE_EXPENSE
) {
    companion object {
        const val TRANSACTION_TYPE_EXPENSE = "EXPENSE"
        const val TRANSACTION_TYPE_INCOME = "INCOME"
    }
}
