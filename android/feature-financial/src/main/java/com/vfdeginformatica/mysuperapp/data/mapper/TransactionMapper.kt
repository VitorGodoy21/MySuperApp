package com.vfdeginformatica.mysuperapp.data.mapper

import com.vfdeginformatica.mysuperapp.data.remote.dto.TransactionDto
import com.vfdeginformatica.mysuperapp.domain.model.RecurrenceOverride
import com.vfdeginformatica.mysuperapp.domain.model.RecurrenceRules
import com.vfdeginformatica.mysuperapp.domain.model.Transaction
import com.vfdeginformatica.mysuperapp.presentation.screen.new_transaction.contract.TransactionType

fun TransactionDto.toModel(): Transaction {
    val start = RecurrenceRules.parseMonth(recurrenceStart)
        ?: if (isRecurring) RecurrenceRules.monthOf(transactionDate) else null
    return Transaction(
        id = id.orEmpty(),
        title = title.orEmpty(),
        description = description.orEmpty(),
        type = if (transactionType == TransactionType.INCOME.name) {
            TransactionType.INCOME
        } else {
            TransactionType.EXPENSE
        },
        paymentMethod = paymentMethod,
        amount = amount,
        isRecurring = isRecurring,
        date = transactionDate,
        categoryIds = category.orEmpty(),
        status = status.orEmpty(),
        cardId = cardId.orEmpty(),
        installments = installments,
        installmentNumber = installmentNumber,
        installmentGroupId = installmentGroupId,
        invoiceMonth = invoiceMonth,
        recurrenceStart = start,
        recurrenceEnd = RecurrenceRules.parseMonth(recurrenceEnd),
        excludedMonths = excludedMonths.orEmpty()
            .mapNotNull { RecurrenceRules.parseMonth(it) }
            .toSet(),
        overrides = overrides.orEmpty().mapNotNull { (key, value) ->
            val month = RecurrenceRules.parseMonth(key) ?: return@mapNotNull null
            val overrideAmount = (value["amount"] as? Number)?.toDouble() ?: return@mapNotNull null
            val overrideTitle = value["title"] as? String ?: title.orEmpty()
            month to RecurrenceOverride(amount = overrideAmount, title = overrideTitle)
        }.toMap()
    )
}

/**
 * Converte o modelo de volta para DTO (usado ao duplicar uma recorrente no split
 * "atualizar este e próximos"). `id` não é copiado — o novo documento recebe outro id.
 */
fun Transaction.toDto(): TransactionDto {
    return TransactionDto(
        id = null,
        title = title,
        description = description,
        transactionType = type.name,
        paymentMethod = paymentMethod,
        amount = amount,
        isRecurring = isRecurring,
        transactionDate = date,
        category = categoryIds,
        status = status.ifEmpty { null },
        cardId = cardId.ifEmpty { null },
        invoiceMonth = invoiceMonth,
        installmentGroupId = installmentGroupId,
        installments = installments,
        installmentNumber = installmentNumber,
        recurrenceStart = recurrenceStart?.let { RecurrenceRules.formatMonth(it) },
        recurrenceEnd = recurrenceEnd?.let { RecurrenceRules.formatMonth(it) },
        excludedMonths = excludedMonths.map { RecurrenceRules.formatMonth(it) },
        overrides = overrides.entries.associate { (month, override) ->
            RecurrenceRules.formatMonth(month) to mapOf<String, Any>(
                "amount" to override.amount,
                "title" to override.title
            )
        }
    )
}