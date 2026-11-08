package com.vfdeginformatica.mysuperapp.domain.use_case.financial.invoice

import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.data.mapper.toModel
import com.vfdeginformatica.mysuperapp.domain.model.FinancialTotals
import com.vfdeginformatica.mysuperapp.domain.model.Transaction
import com.vfdeginformatica.mysuperapp.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.YearMonth
import javax.inject.Inject

/** Pagamento registrado para a fatura [invoiceMonth] do cartão [cardId], ou `null` se em aberto. */
class GetInvoicePaymentUseCase @Inject constructor(
    private val repository: TransactionRepository,
) {
    operator fun invoke(cardId: String, invoiceMonth: YearMonth): Flow<Resource<Transaction?>> = flow {
        emit(Resource.Loading())
        when (val result = repository.getInvoicePayments(FinancialTotals.monthKey(invoiceMonth))) {
            is Resource.Success -> emit(
                Resource.Success(result.data.orEmpty().map { it.toModel() }.firstOrNull { it.cardId == cardId })
            )
            is Resource.Error -> emit(Resource.Error(result.message ?: "Erro ao carregar pagamento da fatura"))
            is Resource.Loading -> emit(Resource.Loading())
        }
    }
}
