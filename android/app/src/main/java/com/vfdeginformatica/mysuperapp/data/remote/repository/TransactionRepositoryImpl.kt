package com.vfdeginformatica.mysuperapp.data.remote.repository

import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.data.remote.datasource.TransactionDao
import com.vfdeginformatica.mysuperapp.data.remote.dto.TransactionDto
import com.vfdeginformatica.mysuperapp.domain.repository.TransactionRepository

class TransactionRepositoryImpl(
    private val transactionDao: TransactionDao
) : TransactionRepository {
    override suspend fun newTransaction(transaction: TransactionDto): Resource<Unit> {
        return transactionDao.addTransaction(transaction)
    }
}