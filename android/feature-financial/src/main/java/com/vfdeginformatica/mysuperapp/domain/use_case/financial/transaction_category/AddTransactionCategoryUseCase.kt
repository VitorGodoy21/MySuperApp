package com.vfdeginformatica.mysuperapp.domain.use_case.financial.transaction_category

import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.data.mapper.toDto
import com.vfdeginformatica.mysuperapp.domain.model.TransactionCategory
import com.vfdeginformatica.mysuperapp.domain.repository.CategoryTransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class AddTransactionCategoryUseCase @Inject constructor(
    private val repository: CategoryTransactionRepository
) {
    operator fun invoke(category: TransactionCategory): Flow<Resource<String>> = flow {
        emit(Resource.Loading())
        if (category.title.isBlank()) {
            emit(Resource.Error("Informe o título da categoria"))
            return@flow
        }
        when (val result = repository.addCategory(category.toDto())) {
            is Resource.Success -> emit(Resource.Success(result.data.orEmpty()))
            is Resource.Error -> emit(Resource.Error(result.message ?: "Erro ao criar categoria"))
            is Resource.Loading -> emit(Resource.Loading())
        }
    }
}
