package com.vfdeginformatica.mysuperapp.domain.use_case.financial.transaction

import com.google.firebase.Timestamp
import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.data.mapper.toModel
import com.vfdeginformatica.mysuperapp.domain.model.Transaction
import com.vfdeginformatica.mysuperapp.domain.model.materializeFor
import com.vfdeginformatica.mysuperapp.domain.model.occursIn
import com.vfdeginformatica.mysuperapp.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.YearMonth
import java.time.ZoneId
import java.util.Date
import javax.inject.Inject

class GetTransactionsUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    operator fun invoke(month: YearMonth): Flow<Resource<List<Transaction>>> = flow {
        emit(Resource.Loading())

        val zone = ZoneId.systemDefault()
        val start = Timestamp(Date.from(month.atDay(1).atStartOfDay(zone).toInstant()))
        val end = Timestamp(Date.from(month.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant()))

        when (val result = repository.getTransactions(start, end)) {
            is Resource.Success -> {
                val monthTransactions = result.data.orEmpty().map { it.toModel() }
                when (val recurringResult = repository.getRecurringTransactions()) {
                    is Resource.Success -> {
                        emit(
                            Resource.Success(
                                mergeWithRecurring(
                                    month = month,
                                    monthTransactions = monthTransactions,
                                    recurring = recurringResult.data.orEmpty().map { it.toModel() }
                                )
                            )
                        )
                    }

                    is Resource.Error -> {
                        emit(Resource.Error(recurringResult.message ?: "Erro ao buscar recorrentes"))
                    }

                    is Resource.Loading -> emit(Resource.Loading())
                }
            }

            is Resource.Error -> {
                emit(Resource.Error(result.message ?: "Erro ao buscar transações"))
            }

            is Resource.Loading -> {
                emit(Resource.Loading())
            }
        }
    }

    companion object {
        /**
         * Junta as transações do mês (query por data) com as ocorrências virtuais das
         * recorrentes. Recorrentes já vindas pela query de data são descartadas em favor
         * da instância materializada, para que overrides/exclusões valham também no mês
         * de origem. Ordena por data decrescente.
         */
        fun mergeWithRecurring(
            month: YearMonth,
            monthTransactions: List<Transaction>,
            recurring: List<Transaction>
        ): List<Transaction> {
            val occurrences = recurring
                .filter { it.occursIn(month) }
                .map { it.materializeFor(month) }
            val nonRecurring = monthTransactions.filterNot { it.isRecurring }
            return (nonRecurring + occurrences)
                .sortedByDescending { it.date?.toDate()?.time ?: Long.MIN_VALUE }
        }
    }
}
