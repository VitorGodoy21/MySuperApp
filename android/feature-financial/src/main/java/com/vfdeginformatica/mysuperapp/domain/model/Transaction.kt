package com.vfdeginformatica.mysuperapp.domain.model

import com.google.firebase.Timestamp
import com.vfdeginformatica.mysuperapp.presentation.screen.new_transaction.contract.TransactionType
import java.time.YearMonth

data class Transaction(
    val id: String,
    val title: String,
    val description: String,
    val type: TransactionType,
    val paymentMethod: String,
    val amount: Double,
    val isRecurring: Boolean,
    val date: Timestamp?,
    val categoryIds: List<String>,
    val status: String,
    val cardId: String,
    val installments: Int?,
    val installmentNumber: Int?,
    val installmentGroupId: String?,
    val invoiceMonth: String?,
    // Recorrência: primeiro mês (null em não recorrentes), último mês inclusive (null = sem fim)
    val recurrenceStart: YearMonth? = null,
    val recurrenceEnd: YearMonth? = null,
    val excludedMonths: Set<YearMonth> = emptySet(),
    val overrides: Map<YearMonth, RecurrenceOverride> = emptyMap(),
    // Mês da ocorrência materializada exibida na tela (null quando não é uma instância virtual)
    val occurrenceMonth: YearMonth? = null
)
