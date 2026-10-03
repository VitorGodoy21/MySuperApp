package com.vfdeginformatica.mysuperapp.data.remote.repository

import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.data.remote.datasource.CategoryTransactionDao
import com.vfdeginformatica.mysuperapp.data.remote.dto.CategoryTransactionDto
import com.vfdeginformatica.mysuperapp.domain.repository.CategoryTransactionRepository

class CategoryTransactionRepositoryImpl(
    private val categoryTransactionDao: CategoryTransactionDao
) : CategoryTransactionRepository {
    override suspend fun getCategoriesTransaction(): List<CategoryTransactionDto>? {
        return categoryTransactionDao.getCategoriesTransaction()
    }

    override suspend fun addCategory(category: CategoryTransactionDto): Resource<String> {
        return categoryTransactionDao.addCategory(category)
    }

    override suspend fun updateCategory(
        id: String,
        title: String,
        description: String,
        transactionType: String,
    ): Resource<Unit> {
        return categoryTransactionDao.updateCategory(id, title, description, transactionType)
    }

    override suspend fun deleteCategory(id: String): Resource<Unit> {
        return categoryTransactionDao.deleteCategory(id)
    }
}