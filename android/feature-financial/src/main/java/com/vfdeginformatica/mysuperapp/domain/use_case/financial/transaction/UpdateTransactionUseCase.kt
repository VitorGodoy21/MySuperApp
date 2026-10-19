package com.vfdeginformatica.mysuperapp.domain.use_case.financial.transaction

import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.domain.model.Transaction
import com.vfdeginformatica.mysuperapp.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * Atualiza título/valor/categorias de uma transação não recorrente. Quando a transação é uma
 * parcela (`installmentGroupId`) e as categorias mudaram, propaga as categorias para todas as
 * parcelas do grupo.
 */
class UpdateTransactionUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    operator fun invoke(
        transaction: Transaction,
        title: String,
        amount: Double,
        categoryIds: List<String>
    ): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())

        if (title.isBlank() || amount <= 0.0) {
            emit(Resource.Error("Informe título e valor válidos"))
            return@flow
        }

        val result = repository.updateTransaction(transaction.id, title, amount, categoryIds)
        if (result is Resource.Error) {
            emit(Resource.Error(result.message ?: "Erro ao atualizar transação"))
            return@flow
        }

        val groupId = transaction.installmentGroupId
        val categoriesChanged = categoryIds.toSet() != transaction.categoryIds.toSet()
        if (groupId != null && categoriesChanged) {
            val groupResult = repository.updateCategoriesForInstallmentGroup(groupId, categoryIds)
            if (groupResult is Resource.Error) {
                emit(Resource.Error(groupResult.message ?: "Erro ao atualizar categorias das parcelas"))
                return@flow
            }
        }

        emit(Resource.Success(Unit))
    }
}
