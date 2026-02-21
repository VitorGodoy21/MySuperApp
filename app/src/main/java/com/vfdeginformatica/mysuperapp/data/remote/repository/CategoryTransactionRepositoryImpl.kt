package com.vfdeginformatica.mysuperapp.data.remote.repository

import com.vfdeginformatica.mysuperapp.data.remote.datasource.CategoryTransactionDao
import com.vfdeginformatica.mysuperapp.data.remote.dto.CategoryTransactionDto
import com.vfdeginformatica.mysuperapp.domain.repository.CategoryTransactionRepository

class CategoryTransactionRepositoryImpl(
    private val categoryTransactionDao: CategoryTransactionDao
) : CategoryTransactionRepository {
    override suspend fun getCategoriesTransaction(): List<CategoryTransactionDto>? {
        return categoryTransactionDao.getCategoriesTransaction()
    }
}