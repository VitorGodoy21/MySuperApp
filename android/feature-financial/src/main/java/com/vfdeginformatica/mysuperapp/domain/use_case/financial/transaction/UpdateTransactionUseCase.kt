package com.vfdeginformatica.mysuperapp.domain.use_case.financial.transaction

import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class UpdateTransactionUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    operator fun invoke(id: String, title: String, amount: Double): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        when (val result = repository.updateTransaction(id, title, amount)) {
            is Resource.Success -> emit(Resource.Success(Unit))
            is Resource.Error -> emit(Resource.Error(result.message ?: "Erro ao atualizar transação"))
            is Resource.Loading -> emit(Resource.Loading())
        }
    }
}
