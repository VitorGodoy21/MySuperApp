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
        amount: Double,
        category: List<String>
    ): Resource<Unit> {
        return transactionDao.updateTransaction(id, title, amount, category)
    }

    override suspend fun updateTransactionCategories(
        id: String,
        category: List<String>
    ): Resource<Unit> {
        return transactionDao.updateTransactionCategories(id, category)
    }

    override suspend fun updateCategoriesForInstallmentGroup(
        groupId: String,
        category: List<String>
    ): Resource<Unit> {
        return transactionDao.updateCategoriesForInstallmentGroup(groupId, category)
    }

    override suspend fun deleteTransaction(id: String): Resource<Unit> {
        return transactionDao.deleteTransaction(id)
    }

    override suspend fun removeCategoryFromTransactions(categoryId: String): Resource<Unit> {
        return transactionDao.removeCategoryFromTransactions(categoryId)
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

    override suspend fun excludeRecurrenceMonth(id: String, month: String): Resource<Unit> {
        return transactionDao.excludeRecurrenceMonth(id, month)
    }

    override suspend fun endRecurrence(id: String, lastMonth: String): Resource<Unit> {
        return transactionDao.endRecurrence(id, lastMonth)
    }

    override suspend fun overrideRecurrenceMonth(
        id: String,
        month: String,
        title: String,
        amount: Double
    ): Resource<Unit> {
        return transactionDao.overrideRecurrenceMonth(id, month, title, amount)
    }

    override suspend fun updateRecurrenceTemplate(
        id: String,
        title: String,
        amount: Double,
        category: List<String>,
        excludedMonths: List<String>,
        overrides: Map<String, Map<String, Any>>
    ): Resource<Unit> {
        return transactionDao.updateRecurrenceTemplate(
            id, title, amount, category, excludedMonths, overrides
        )
    }

    override suspend fun splitRecurrence(
        originalId: String,
        lastMonthOfOriginal: String,
        newTransaction: TransactionDto,
        originalCategory: List<String>
    ): Resource<Unit> {
        return transactionDao.splitRecurrence(
            originalId, lastMonthOfOriginal, newTransaction, originalCategory
        )
    }
}