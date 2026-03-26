package com.vfdeginformatica.mysuperapp.domain.repository

import com.vfdeginformatica.mysuperapp.data.remote.dto.CategoryTransactionDto

interface CategoryTransactionRepository {
    suspend fun getCategoriesTransaction(): List<CategoryTransactionDto>?
}