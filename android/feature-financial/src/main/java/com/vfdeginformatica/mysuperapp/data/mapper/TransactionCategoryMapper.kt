package com.vfdeginformatica.mysuperapp.data.mapper

import com.vfdeginformatica.mysuperapp.data.remote.dto.CategoryTransactionDto
import com.vfdeginformatica.mysuperapp.domain.model.TransactionCategory
import com.vfdeginformatica.mysuperapp.presentation.screen.new_transaction.contract.TransactionType

/** Converte a string persistida em `transactionType`; valores ausentes/desconhecidos viram EXPENSE. */
fun String?.toTransactionType(): TransactionType =
    if (this == TransactionType.INCOME.name) TransactionType.INCOME else TransactionType.EXPENSE

fun CategoryTransactionDto.toModel(): TransactionCategory {
    return TransactionCategory(
        id = id,
        title = title,
        description = description,
        type = transactionType.toTransactionType(),
    )
}

fun TransactionCategory.toDto(): CategoryTransactionDto {
    return CategoryTransactionDto(
        id = id,
        title = title,
        description = description,
        transactionType = type.name,
    )
}
