package com.vfdeginformatica.mysuperapp.data.remote.datasource

import com.google.firebase.Timestamp
import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.data.remote.dto.TransactionDto

interface TransactionDao {
    suspend fun addTransaction(transaction: TransactionDto): Resource<Unit>
    suspend fun addTransactions(transactions: List<TransactionDto>): Resource<Unit>
    suspend fun updateTransaction(id: String, title: String, amount: Double): Resource<Unit>
    suspend fun deleteTransaction(id: String): Resource<Unit>
    suspend fun removeCategoryFromTransactions(categoryId: String): Resource<Unit>
    suspend fun getTransactions(start: Timestamp, end: Timestamp): Resource<List<TransactionDto>>
    suspend fun getTransactionsByInvoiceMonth(invoiceMonth: String): Resource<List<TransactionDto>>
    suspend fun getRecurringTransactions(): Resource<List<TransactionDto>>
}