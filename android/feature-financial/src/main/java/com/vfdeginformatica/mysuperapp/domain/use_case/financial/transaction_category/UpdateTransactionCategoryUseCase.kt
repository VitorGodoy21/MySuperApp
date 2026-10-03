package com.vfdeginformatica.mysuperapp.domain.use_case.financial.transaction_category

import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.domain.model.TransactionCategory
import com.vfdeginformatica.mysuperapp.domain.repository.CategoryTransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class UpdateTransactionCategoryUseCase @Inject constructor(
    private val repository: CategoryTransactionRepository
) {
    operator fun invoke(category: TransactionCategory): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        if (category.id.isBlank()) {
            emit(Resource.Error("Categoria inválida"))
            return@flow
        }
        if (category.title.isBlank()) {
            emit(Resource.Error("Informe o título da categoria"))
            return@flow
        }
        val result = repository.updateCategory(
            id = category.id,
            title = category.title,
            description = category.description,
            transactionType = category.type.name,
        )
        when (result) {
            is Resource.Success -> emit(Resource.Success(Unit))
            is Resource.Error -> emit(Resource.Error(result.message ?: "Erro ao atualizar categoria"))
            is Resource.Loading -> emit(Resource.Loading())
        }
    }
}
