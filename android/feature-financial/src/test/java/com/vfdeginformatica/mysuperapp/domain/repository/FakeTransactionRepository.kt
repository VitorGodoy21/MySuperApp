package com.vfdeginformatica.mysuperapp.domain.repository

import com.google.firebase.Timestamp
import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.data.remote.dto.TransactionDto

/** Fake de [TransactionRepository] para testes: registra chamadas e devolve resultados configuráveis. */
open class FakeTransactionRepository : TransactionRepository {
    val added = mutableListOf<TransactionDto>()
    val updates = mutableListOf<UpdateCall>()
    val categoryUpdates = mutableListOf<Pair<String, List<String>>>()
    val groupCategoryUpdates = mutableListOf<Pair<String, List<String>>>()
    val deletedIds = mutableListOf<String>()
    val excluded = mutableListOf<Pair<String, String>>()
    val ended = mutableListOf<Pair<String, String>>()
    val overridden = mutableListOf<OverrideCall>()
    val templateUpdates = mutableListOf<TemplateUpdateCall>()
    val splits = mutableListOf<SplitCall>()
    val cleanedIds = mutableListOf<String>()

    var monthTransactions: List<TransactionDto> = emptyList()
    var recurringTransactions: List<TransactionDto> = emptyList()
    var writeResult: Resource<Unit> = Resource.Success(Unit)
    var groupCategoryResult: Resource<Unit> = Resource.Success(Unit)
    var cleanupResult: Resource<Unit> = Resource.Success(Unit)

    data class UpdateCall(val id: String, val title: String, val amount: Double, val category: List<String>)
    data class OverrideCall(val id: String, val month: String, val title: String, val amount: Double)
    data class TemplateUpdateCall(
        val id: String,
        val title: String,
        val amount: Double,
        val category: List<String>,
        val excludedMonths: List<String>,
        val overrides: Map<String, Map<String, Any>>
    )

    data class SplitCall(
        val originalId: String,
        val lastMonth: String,
        val newTransaction: TransactionDto,
        val originalCategory: List<String>
    )

    override suspend fun newTransaction(transaction: TransactionDto): Resource<Unit> {
        added += transaction
        return writeResult
    }

    override suspend fun newTransactions(transactions: List<TransactionDto>): Resource<Unit> {
        added += transactions
        return writeResult
    }

    override suspend fun updateTransaction(
        id: String,
        title: String,
        amount: Double,
        category: List<String>
    ): Resource<Unit> {
        updates += UpdateCall(id, title, amount, category)
        return writeResult
    }

    override suspend fun updateTransactionCategories(id: String, category: List<String>): Resource<Unit> {
        categoryUpdates += id to category
        return writeResult
    }

    override suspend fun updateCategoriesForInstallmentGroup(
        groupId: String,
        category: List<String>
    ): Resource<Unit> {
        groupCategoryUpdates += groupId to category
        return groupCategoryResult
    }

    override suspend fun deleteTransaction(id: String): Resource<Unit> {
        deletedIds += id
        return writeResult
    }

    override suspend fun removeCategoryFromTransactions(categoryId: String): Resource<Unit> {
        cleanedIds += categoryId
        return cleanupResult
    }

    override suspend fun getTransactions(start: Timestamp, end: Timestamp): Resource<List<TransactionDto>> =
        Resource.Success(monthTransactions)

    override suspend fun getTransactionsByInvoiceMonth(invoiceMonth: String): Resource<List<TransactionDto>> =
        Resource.Success(emptyList())

    override suspend fun getRecurringTransactions(): Resource<List<TransactionDto>> =
        Resource.Success(recurringTransactions)

    override suspend fun excludeRecurrenceMonth(id: String, month: String): Resource<Unit> {
        excluded += id to month
        return writeResult
    }

    override suspend fun endRecurrence(id: String, lastMonth: String): Resource<Unit> {
        ended += id to lastMonth
        return writeResult
    }

    override suspend fun overrideRecurrenceMonth(
        id: String,
        month: String,
        title: String,
        amount: Double
    ): Resource<Unit> {
        overridden += OverrideCall(id, month, title, amount)
        return writeResult
    }

    override suspend fun updateRecurrenceTemplate(
        id: String,
        title: String,
        amount: Double,
        category: List<String>,
        excludedMonths: List<String>,
        overrides: Map<String, Map<String, Any>>
    ): Resource<Unit> {
        templateUpdates += TemplateUpdateCall(id, title, amount, category, excludedMonths, overrides)
        return writeResult
    }

    override suspend fun splitRecurrence(
        originalId: String,
        lastMonthOfOriginal: String,
        newTransaction: TransactionDto,
        originalCategory: List<String>
    ): Resource<Unit> {
        splits += SplitCall(originalId, lastMonthOfOriginal, newTransaction, originalCategory)
        return writeResult
    }
}
