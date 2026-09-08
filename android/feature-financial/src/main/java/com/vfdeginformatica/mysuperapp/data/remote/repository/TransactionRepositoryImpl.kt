package com.vfdeginformatica.mysuperapp.data.remote.repository

import com.google.firebase.Timestamp
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

    override suspend fun newTransactions(transactions: List<TransactionDto>): Resource<Unit> {
        return transactionDao.addTransactions(transactions)
    }

    override suspend fun updateTransaction(
        id: String,
        title: String,
        amount: Double
    ): Resource<Unit> {
        return transactionDao.updateTransaction(id, title, amount)
    }

    override suspend fun deleteTransaction(id: String): Resource<Unit> {
        return transactionDao.deleteTransaction(id)
    }

    override suspend fun getTransactions(
        start: Timestamp,
        end: Timestamp
    ): Resource<List<TransactionDto>> {
        return transactionDao.getTransactions(start, end)
    }

    override suspend fun getTransactionsByInvoiceMonth(
        invoiceMonth: String
    ): Resource<List<TransactionDto>> {
        return transactionDao.getTransactionsByInvoiceMonth(invoiceMonth)
    }

    override suspend fun getRecurringTransactions(): Resource<List<TransactionDto>> {
        return transactionDao.getRecurringTransactions()
    }
}