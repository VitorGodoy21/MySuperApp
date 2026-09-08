package com.vfdeginformatica.mysuperapp.domain.use_case.financial.transaction

import com.google.firebase.Timestamp
import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.data.remote.dto.TransactionDto
import com.vfdeginformatica.mysuperapp.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject

class NewTransactionUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    operator fun invoke(
        title: String? = null,
        description: String? = null,
        transactionType: String? = null,
        paymentMethod: String,
        amount: Double,
        isRecurring: Boolean,
        transactionDate: Timestamp? = null,
        category: List<String>? = null,
        status: String? = null,
        cardId: String? = null,
        installments: Int? = null,
        // Dia de fechamento da fatura do cartão selecionado (0 = sem fechamento)
        closingDay: Int = 0,
    ): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())

        val isCredit = transactionType == "EXPENSE" && paymentMethod == "CREDIT"
        val baseInvoiceMonth = if (isCredit) {
            computeInvoiceMonth(transactionDate ?: Timestamp.now(), closingDay)
        } else {
            null
        }

        val baseDto = TransactionDto(
            title = title,
            description = description,
            transactionType = transactionType,
            paymentMethod = paymentMethod,
            amount = amount,
            isRecurring = isRecurring,
            transactionDate = transactionDate,
            category = category,
            status = status,
            cardId = cardId,
            invoiceMonth = baseInvoiceMonth?.format(INVOICE_MONTH_FORMAT),
            installmentGroupId = null,
            installments = installments,
        )

        val totalInstallments = installments ?: 1
        val result = if (isCredit && !isRecurring && totalInstallments > 1 && baseInvoiceMonth != null) {
            // Parcelamento: N documentos (amount/N), um por fatura, ligados pelo mesmo grupo
            val groupId = UUID.randomUUID().toString()
            val installmentAmount = BigDecimal(amount)
                .divide(BigDecimal(totalInstallments), 2, RoundingMode.DOWN)
                .toDouble()
            val lastInstallmentAmount = BigDecimal(amount)
                .subtract(BigDecimal(installmentAmount).multiply(BigDecimal(totalInstallments - 1)))
                .setScale(2, RoundingMode.HALF_UP)
                .toDouble()

            val docs = (1..totalInstallments).map { number ->
                baseDto.copy(
                    amount = if (number == totalInstallments) lastInstallmentAmount else installmentAmount,
                    invoiceMonth = baseInvoiceMonth.plusMonths((number - 1).toLong())
                        .format(INVOICE_MONTH_FORMAT),
                    installmentGroupId = groupId,
                    installments = totalInstallments,
                    installmentNumber = number,
                )
            }
            repository.newTransactions(docs)
        } else {
            repository.newTransaction(baseDto)
        }

        when (result) {
            is Resource.Success -> emit(Resource.Success(Unit))
            is Resource.Error -> emit(Resource.Error(result.message ?: "An unexpected error occurred"))
            is Resource.Loading -> emit(Resource.Loading())
        }
    }

    companion object {
        val INVOICE_MONTH_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM")

        /**
         * Fatura de destino de uma compra no crédito: compras no dia de fechamento
         * ou depois dele entram na fatura do mês seguinte.
         */
        fun computeInvoiceMonth(date: Timestamp, closingDay: Int): YearMonth {
            val localDate: LocalDate = date.toDate().toInstant()
                .atZone(ZoneId.systemDefault()).toLocalDate()
            val month = YearMonth.from(localDate)
            return if (closingDay in 1..31 && localDate.dayOfMonth >= closingDay) {
                month.plusMonths(1)
            } else {
                month
            }
        }
    }
}