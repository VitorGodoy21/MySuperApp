package com.vfdeginformatica.mysuperapp.domain.use_case.financial.invoice

import com.google.firebase.Timestamp
import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.data.remote.dto.TransactionDto
import com.vfdeginformatica.mysuperapp.domain.model.FinancialTotals
import com.vfdeginformatica.mysuperapp.domain.model.PaidInvoice
import com.vfdeginformatica.mysuperapp.domain.repository.TransactionRepository
import com.vfdeginformatica.mysuperapp.presentation.screen.new_transaction.contract.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * Registra o pagamento de uma fatura como uma transação EXPENSE com
 * `paymentMethod = INVOICE_PAYMENT`. No saldo de caixa o valor pago substitui o total
 * projetado da fatura (nunca soma), por isso a transação não recebe `invoiceMonth`.
 */
class PayInvoiceUseCase @Inject constructor(
    private val repository: TransactionRepository,
) {
    operator fun invoke(
        invoice: PaidInvoice,
        amount: Double,
        date: Timestamp,
    ): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        if (amount <= 0.0) {
            emit(Resource.Error("Informe um valor maior que zero"))
            return@flow
        }
        if (invoice.cardId.isBlank()) {
            emit(Resource.Error("Fatura sem cartão não pode ser paga pelo app"))
            return@flow
        }
        if (invoice.isPaid) {
            emit(Resource.Error("Esta fatura já foi paga"))
            return@flow
        }
        when (val result = repository.newTransaction(buildDto(invoice, amount, date))) {
            is Resource.Success -> emit(Resource.Success(Unit))
            is Resource.Error -> emit(Resource.Error(result.message ?: "Erro ao registrar pagamento"))
            is Resource.Loading -> emit(Resource.Loading())
        }
    }

    companion object {
        fun title(bank: String): String = "Pagamento da fatura — $bank"

        fun buildDto(invoice: PaidInvoice, amount: Double, date: Timestamp): TransactionDto = TransactionDto(
            title = title(invoice.bank),
            description = "Fatura de ${invoice.invoiceMonthKey}",
            transactionType = TransactionType.EXPENSE.name,
            paymentMethod = FinancialTotals.INVOICE_PAYMENT,
            amount = amount,
            isRecurring = false,
            transactionDate = date,
            category = emptyList(),
            status = "pending",
            cardId = invoice.cardId,
            invoiceMonth = null,
            invoicePaymentMonth = invoice.invoiceMonthKey,
        )
    }
}
