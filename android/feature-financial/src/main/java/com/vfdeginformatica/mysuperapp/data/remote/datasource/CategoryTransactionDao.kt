package com.vfdeginformatica.mysuperapp.data.remote.datasource

import com.vfdeginformatica.mysuperapp.data.remote.dto.CategoryTransactionDto

interface CategoryTransactionDao {
    suspend fun getCategoriesTransaction(): List<CategoryTransactionDto>?
}