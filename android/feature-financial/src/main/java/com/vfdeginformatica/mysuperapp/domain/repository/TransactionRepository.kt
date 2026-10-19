package com.vfdeginformatica.mysuperapp.domain.repository

import com.google.firebase.Timestamp
import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.data.remote.dto.TransactionDto

interface TransactionRepository {
    suspend fun newTransaction(transaction: TransactionDto): Resource<Unit>
    suspend fun newTransactions(transactions: List<TransactionDto>): Resource<Unit>
    suspend fun updateTransaction(
        id: String,
        title: String,
        amount: Double,
        category: List<String>
    ): Resource<Unit>

    suspend fun updateTransactionCategories(id: String, category: List<String>): Resource<Unit>
    suspend fun updateCategoriesForInstallmentGroup(
        groupId: String,
        category: List<String>
    ): Resource<Unit>

    suspend fun deleteTransaction(id: String): Resource<Unit>
    suspend fun removeCategoryFromTransactions(categoryId: String): Resource<Unit>
    suspend fun getTransactions(start: Timestamp, end: Timestamp): Resource<List<TransactionDto>>
    suspend fun getTransactionsByInvoiceMonth(invoiceMonth: String): Resource<List<TransactionDto>>
    suspend fun getRecurringTransactions(): Resource<List<TransactionDto>>

    // ---- Recorrência (meses no formato yyyy-MM) ----
    suspend fun excludeRecurrenceMonth(id: String, month: String): Resource<Unit>
    suspend fun endRecurrence(id: String, lastMonth: String): Resource<Unit>
    suspend fun overrideRecurrenceMonth(
        id: String,
        month: String,
        title: String,
        amount: Double
    ): Resource<Unit>

    suspend fun updateRecurrenceTemplate(
        id: String,
        title: String,
        amount: Double,
        category: List<String>,
        excludedMonths: List<String>,
        overrides: Map<String, Map<String, Any>>
    ): Resource<Unit>

    suspend fun splitRecurrence(
        originalId: String,
        lastMonthOfOriginal: String,
        newTransaction: TransactionDto,
        originalCategory: List<String>
    ): Resource<Unit>
}