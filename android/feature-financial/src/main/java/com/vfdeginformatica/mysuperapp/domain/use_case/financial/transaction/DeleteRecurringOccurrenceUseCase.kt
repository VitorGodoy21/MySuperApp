package com.vfdeginformatica.mysuperapp.domain.use_case.financial.transaction

import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.domain.model.RecurrenceEditScope
import com.vfdeginformatica.mysuperapp.domain.model.RecurrenceRules
import com.vfdeginformatica.mysuperapp.domain.model.Transaction
import com.vfdeginformatica.mysuperapp.domain.model.effectiveRecurrenceStart
import com.vfdeginformatica.mysuperapp.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.YearMonth
import javax.inject.Inject

/**
 * Exclui uma ocorrência de transação recorrente.
 *
 * - [RecurrenceEditScope.THIS_MONTH]: adiciona o mês a `excludedMonths`.
 * - [RecurrenceEditScope.THIS_AND_FUTURE]: se o mês for o inicial, apaga o documento;
 *   caso contrário encerra a recorrência no mês anterior (meses anteriores mantidos).
 */
class DeleteRecurringOccurrenceUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    operator fun invoke(
        transaction: Transaction,
        month: YearMonth,
        scope: RecurrenceEditScope
    ): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())

        if (!transaction.isRecurring || transaction.id.isBlank()) {
            emit(Resource.Error("Transação não é recorrente"))
            return@flow
        }

        val start = transaction.effectiveRecurrenceStart
        val result = when (scope) {
            RecurrenceEditScope.THIS_MONTH ->
                repository.excludeRecurrenceMonth(
                    transaction.id,
                    RecurrenceRules.formatMonth(month)
                )

            RecurrenceEditScope.THIS_AND_FUTURE ->
                if (start == null || month <= start) {
                    repository.deleteTransaction(transaction.id)
                } else {
                    repository.endRecurrence(
                        transaction.id,
                        RecurrenceRules.formatMonth(month.minusMonths(1))
                    )
                }
        }

        when (result) {
            is Resource.Success -> emit(Resource.Success(Unit))
            is Resource.Error -> emit(Resource.Error(result.message ?: "Erro ao excluir recorrência"))
            is Resource.Loading -> emit(Resource.Loading())
        }
    }
}
