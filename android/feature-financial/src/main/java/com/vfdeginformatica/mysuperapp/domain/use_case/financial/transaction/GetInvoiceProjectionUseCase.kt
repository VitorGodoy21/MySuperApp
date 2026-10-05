package com.vfdeginformatica.mysuperapp.domain.use_case.financial.transaction

import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.data.mapper.toModel
import com.vfdeginformatica.mysuperapp.domain.model.InvoiceProjection
import com.vfdeginformatica.mysuperapp.domain.model.materializeFor
import com.vfdeginformatica.mysuperapp.domain.model.occursIn
import com.vfdeginformatica.mysuperapp.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.YearMonth
import javax.inject.Inject

class GetInvoiceProjectionUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    operator fun invoke(targetMonth: YearMonth): Flow<Resource<InvoiceProjection>> = flow {
        emit(Resource.Loading())

        val monthKey = targetMonth.format(NewTransactionUseCase.INVOICE_MONTH_FORMAT)

        val invoiceResult = repository.getTransactionsByInvoiceMonth(monthKey)
        if (invoiceResult is Resource.Error) {
            emit(Resource.Error(invoiceResult.message ?: "Erro ao carregar fatura"))
            return@flow
        }

        val recurringResult = repository.getRecurringTransactions()
        if (recurringResult is Resource.Error) {
            emit(Resource.Error(recurringResult.message ?: "Erro ao carregar recorrentes"))
            return@flow
        }

        emit(
            Resource.Success(
                InvoiceProjection(
                    invoiceTransactions = invoiceResult.data.orEmpty().map { it.toModel() },
                    // Só recorrentes com ocorrência no mês alvo, já com override/exclusão aplicados
                    recurringTransactions = recurringResult.data.orEmpty()
                        .map { it.toModel() }
                        .filter { it.occursIn(targetMonth) }
                        .map { it.materializeFor(targetMonth) }
                )
            )
        )
    }
}
