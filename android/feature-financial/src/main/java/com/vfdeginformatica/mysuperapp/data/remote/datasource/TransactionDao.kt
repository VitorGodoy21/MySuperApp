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

    // ---- Recorrência (meses no formato yyyy-MM) ----

    /** Exclui apenas a ocorrência do mês informado (arrayUnion em excludedMonths). */
    suspend fun excludeRecurrenceMonth(id: String, month: String): Resource<Unit>

    /** Encerra a recorrência: lastMonth passa a ser o último mês com ocorrência. */
    suspend fun endRecurrence(id: String, lastMonth: String): Resource<Unit>

    /** Ajusta título/valor somente no mês informado (overrides[month]). */
    suspend fun overrideRecurrenceMonth(
        id: String,
        month: String,
        title: String,
        amount: Double
    ): Resource<Unit>

    /**
     * Atualiza título/valor do documento-modelo e substitui excludedMonths/overrides
     * (usado quando "este e próximos" começa no próprio mês inicial da recorrência).
     */
    suspend fun updateRecurrenceTemplate(
        id: String,
        title: String,
        amount: Double,
        excludedMonths: List<String>,
        overrides: Map<String, Map<String, Any>>
    ): Resource<Unit>

    /**
     * Split atômico: encerra o documento original em lastMonthOfOriginal e cria
     * um novo documento recorrente a partir do mês seguinte.
     */
    suspend fun splitRecurrence(
        originalId: String,
        lastMonthOfOriginal: String,
        newTransaction: TransactionDto
    ): Resource<Unit>
}