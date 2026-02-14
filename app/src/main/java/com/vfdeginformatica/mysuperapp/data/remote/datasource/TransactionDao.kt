package com.vfdeginformatica.mysuperapp.data.remote.datasource

import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.data.remote.dto.TransactionDto

interface TransactionDao {
    suspend fun addTransaction(transaction: TransactionDto): Resource<Unit>
}