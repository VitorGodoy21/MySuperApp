package com.vfdeginformatica.mysuperapp.domain.repository

import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.data.remote.dto.TransactionDto

interface TransactionRepository {
    suspend fun newTransaction(transaction: TransactionDto): Resource<Unit>
}