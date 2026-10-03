package com.vfdeginformatica.mysuperapp.domain.repository

import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.data.remote.dto.CategoryTransactionDto

interface CategoryTransactionRepository {
    suspend fun getCategoriesTransaction(): List<CategoryTransactionDto>?
    suspend fun addCategory(category: CategoryTransactionDto): Resource<String>
    suspend fun updateCategory(
        id: String,
        title: String,
        description: String,
        transactionType: String,
    ): Resource<Unit>
    suspend fun deleteCategory(id: String): Resource<Unit>
}