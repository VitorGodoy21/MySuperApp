package com.vfdeginformatica.mysuperapp.data.remote.dto

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName

@IgnoreExtraProperties
data class TransactionDto(
    @DocumentId var id: String? = null,
    var title: String? = null,
    var description: String? = null,
    var transactionType: String? = null, //INCOME, EXPENSE
    var paymentMethod: String = "", //CREDIT, DEBIT
    var amount: Double = 0.0,
    // PropertyName preserva o nome "isRecurring" gravado no documento
    // (o bean mapping do Firestore inferiria "recurring" para boolean com prefixo is)
    @get:PropertyName("isRecurring")
    @set:PropertyName("isRecurring")
    var isRecurring: Boolean = false,
    var transactionDate: Timestamp? = null,
    var category: List<String>? = null,
    var status: String? = null,
    var cardId: String? = null,
    var invoiceMonth: String? = null,
    var installmentGroupId: String? = null,
    var installments: Int? = null,
    var installmentNumber: Int? = null,
    // Recorrência (apenas quando isRecurring == true). Meses no formato yyyy-MM.
    // recurrenceStart ausente => mês da transactionDate (compat com documentos antigos).
    var recurrenceStart: String? = null,
    var recurrenceEnd: String? = null,
    var excludedMonths: List<String>? = null,
    // overrides[yyyy-MM] = { "amount": Double, "title": String }
    var overrides: Map<String, Map<String, Any>>? = null,
)
