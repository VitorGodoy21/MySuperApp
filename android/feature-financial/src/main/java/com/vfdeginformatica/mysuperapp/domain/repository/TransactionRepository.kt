package com.vfdeginformatica.mysuperapp.domain.repository

import com.google.firebase.Timestamp
import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.data.remote.dto.TransactionDto

interface TransactionRepository {
    suspend fun newTransaction(transaction: TransactionDto): Resource<Unit>
    suspend fun newTransactions(transactions: List<TransactionDto>): Resource<Unit>
    suspend fun updateTransaction(id: String, title: String, amount: Double): Resource<Unit>
    suspend fun deleteTransaction(id: String): Resource<Unit>
    suspend fun getTransactions(start: Timestamp, end: Timestamp): Resource<List<TransactionDto>>
    suspend fun getTransactionsByInvoiceMonth(invoiceMonth: String): Resource<List<TransactionDto>>
    suspend fun getRecurringTransactions(): Resource<List<TransactionDto>>
}