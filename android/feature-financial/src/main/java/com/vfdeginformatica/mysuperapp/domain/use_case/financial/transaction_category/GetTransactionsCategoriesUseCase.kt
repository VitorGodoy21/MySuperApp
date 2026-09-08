package com.vfdeginformatica.mysuperapp.domain.use_case.financial.transaction_category

import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.data.remote.dto.CategoryTransactionDto
import com.vfdeginformatica.mysuperapp.domain.repository.CategoryTransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class GetTransactionsCategoriesUseCase @Inject constructor(
    private val repository: CategoryTransactionRepository
) {
    operator fun invoke(): Flow<Resource<List<CategoryTransactionDto>>> = flow {
        emit(Resource.Loading())
        val categories = repository.getCategoriesTransaction()
        if (categories != null) {
            emit(Resource.Success(categories))
        } else {
            emit(Resource.Error("An unexpected error occurred"))
        }
    }
}