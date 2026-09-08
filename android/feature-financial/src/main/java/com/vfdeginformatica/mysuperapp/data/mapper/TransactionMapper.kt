package com.vfdeginformatica.mysuperapp.data.mapper

import com.vfdeginformatica.mysuperapp.data.remote.dto.TransactionDto
import com.vfdeginformatica.mysuperapp.domain.model.Transaction
import com.vfdeginformatica.mysuperapp.presentation.screen.new_transaction.contract.TransactionType

fun TransactionDto.toModel(): Transaction {
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
        invoiceMonth = invoiceMonth
    )
}