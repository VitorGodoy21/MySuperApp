package com.vfdeginformatica.mysuperapp.domain.use_case.financial.transaction_category

import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.domain.repository.CategoryTransactionRepository
import com.vfdeginformatica.mysuperapp.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * Exclui uma categoria garantindo que nenhuma transação fique referenciando o id removido.
 * A categoria só é apagada depois que a remoção nas transações é concluída com sucesso;
 * em caso de falha parcial, a categoria permanece e a operação pode ser repetida.
 */
class DeleteTransactionCategoryUseCase @Inject constructor(
    private val categoryRepository: CategoryTransactionRepository,
    private val transactionRepository: TransactionRepository,
) {
    operator fun invoke(categoryId: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        if (categoryId.isBlank()) {
            emit(Resource.Error("Categoria inválida"))
            return@flow
        }
        when (val cleanup = transactionRepository.removeCategoryFromTransactions(categoryId)) {
            is Resource.Error -> {
                emit(Resource.Error(cleanup.message ?: "Erro ao remover categoria das transações"))
                return@flow
            }
            else -> Unit
        }
        when (val result = categoryRepository.deleteCategory(categoryId)) {
            is Resource.Success -> emit(Resource.Success(Unit))
            is Resource.Error -> emit(Resource.Error(result.message ?: "Erro ao excluir categoria"))
            is Resource.Loading -> emit(Resource.Loading())
        }
    }
}
