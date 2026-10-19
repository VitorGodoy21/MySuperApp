package com.vfdeginformatica.mysuperapp.domain.use_case.financial.transaction

import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.data.mapper.toDto
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
 * Atualiza título/valor de uma transação recorrente. As categorias valem sempre para a série
 * inteira (documento-modelo), independentemente do escopo.
 *
 * - [RecurrenceEditScope.THIS_MONTH]: grava `overrides[month]` e, se as categorias mudaram,
 *   atualiza `category` no modelo.
 * - [RecurrenceEditScope.THIS_AND_FUTURE]: se o mês for o inicial, atualiza o próprio
 *   documento-modelo (descartando exceções de meses >= month); caso contrário faz um
 *   split atômico: encerra o original no mês anterior (aplicando as novas categorias) e cria
 *   um novo documento a partir de `month` com os novos valores, herdando apenas as exceções
 *   de meses >= month.
 */
class UpdateRecurringOccurrenceUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    operator fun invoke(
        transaction: Transaction,
        month: YearMonth,
        title: String,
        amount: Double,
        categoryIds: List<String>,
        scope: RecurrenceEditScope
    ): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())

        if (!transaction.isRecurring || transaction.id.isBlank()) {
            emit(Resource.Error("Transação não é recorrente"))
            return@flow
        }
        if (title.isBlank() || amount <= 0.0) {
            emit(Resource.Error("Informe título e valor válidos"))
            return@flow
        }

        val result = when (scope) {
            RecurrenceEditScope.THIS_MONTH -> {
                val categoriesChanged = categoryIds.toSet() != transaction.categoryIds.toSet()
                val categoryResult = if (categoriesChanged) {
                    repository.updateTransactionCategories(transaction.id, categoryIds)
                } else {
                    Resource.Success(Unit)
                }
                if (categoryResult is Resource.Error) {
                    categoryResult
                } else {
                    repository.overrideRecurrenceMonth(
                        id = transaction.id,
                        month = RecurrenceRules.formatMonth(month),
                        title = title,
                        amount = amount
                    )
                }
            }

            RecurrenceEditScope.THIS_AND_FUTURE -> {
                val start = transaction.effectiveRecurrenceStart
                // Exceções de meses anteriores ao mês editado continuam valendo no documento
                // original; as de meses >= month seriam sobrescritas pelo novo valor.
                val keptExcluded = transaction.excludedMonths
                    .filter { it < month }
                    .map { RecurrenceRules.formatMonth(it) }
                val keptOverrides = transaction.overrides
                    .filterKeys { it < month }
                    .entries.associate { (key, value) ->
                        RecurrenceRules.formatMonth(key) to mapOf<String, Any>(
                            "amount" to value.amount,
                            "title" to value.title
                        )
                    }

                if (start == null || month <= start) {
                    repository.updateRecurrenceTemplate(
                        id = transaction.id,
                        title = title,
                        amount = amount,
                        category = categoryIds,
                        excludedMonths = keptExcluded,
                        overrides = keptOverrides
                    )
                } else {
                    val newDto = transaction.copy(
                        title = title,
                        amount = amount,
                        categoryIds = categoryIds,
                        date = RecurrenceRules.firstDayTimestamp(month),
                        recurrenceStart = month,
                        recurrenceEnd = null,
                        excludedMonths = emptySet(),
                        overrides = emptyMap(),
                        occurrenceMonth = null
                    ).toDto()
                    repository.splitRecurrence(
                        originalId = transaction.id,
                        lastMonthOfOriginal = RecurrenceRules.formatMonth(month.minusMonths(1)),
                        newTransaction = newDto,
                        originalCategory = categoryIds
                    )
                }
            }
        }

        when (result) {
            is Resource.Success -> emit(Resource.Success(Unit))
            is Resource.Error -> emit(Resource.Error(result.message ?: "Erro ao atualizar recorrência"))
            is Resource.Loading -> emit(Resource.Loading())
        }
    }
}
