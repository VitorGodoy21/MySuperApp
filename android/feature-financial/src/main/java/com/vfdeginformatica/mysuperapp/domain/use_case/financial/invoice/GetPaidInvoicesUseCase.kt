package com.vfdeginformatica.mysuperapp.domain.use_case.financial.invoice

import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.data.mapper.toModel
import com.vfdeginformatica.mysuperapp.data.remote.dto.CardDto
import com.vfdeginformatica.mysuperapp.domain.model.FinancialTotals
import com.vfdeginformatica.mysuperapp.domain.model.InvoiceProjection
import com.vfdeginformatica.mysuperapp.domain.model.PaidInvoice
import com.vfdeginformatica.mysuperapp.domain.model.Transaction
import com.vfdeginformatica.mysuperapp.domain.repository.CardRepository
import com.vfdeginformatica.mysuperapp.domain.repository.TransactionRepository
import com.vfdeginformatica.mysuperapp.domain.use_case.financial.transaction.GetInvoiceProjectionUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import java.time.YearMonth
import javax.inject.Inject

/**
 * Faturas que saem da conta em [month], uma por cartão de crédito, cada uma apontando para o
 * `invoiceMonth` certo segundo [FinancialTotals.paidInvoiceMonth] (vence depois do fechamento
 * ⇒ fatura do próprio mês; senão ⇒ fatura do mês anterior) e já ligada ao pagamento registrado
 * (`invoicePaymentMonth == invoiceMonth` do cartão), quando houver. Itens de cartões
 * desconhecidos (excluídos) ficam agrupados na fatura de `M-1`.
 */
class GetPaidInvoicesUseCase @Inject constructor(
    private val cardRepository: CardRepository,
    private val transactionRepository: TransactionRepository,
    private val getInvoiceProjectionUseCase: GetInvoiceProjectionUseCase,
) {
    operator fun invoke(month: YearMonth): Flow<Resource<List<PaidInvoice>>> = flow {
        emit(Resource.Loading())

        val cards = cardRepository.getCards()
        if (cards == null) {
            emit(Resource.Error("Erro ao carregar cartões"))
            return@flow
        }

        val creditCards = cards.filter { FinancialTotals.CREDIT in it.paymentMethods }
        val monthsToLoad = (creditCards.map { paidMonthOf(it, month) } + month.minusMonths(1)).toSet()

        val projections = mutableMapOf<YearMonth, InvoiceProjection>()
        val payments = mutableListOf<Transaction>()
        for (target in monthsToLoad) {
            when (val result = getInvoiceProjectionUseCase(target).first { it !is Resource.Loading }) {
                is Resource.Success -> projections[target] = result.data ?: InvoiceProjection(emptyList(), emptyList())
                else -> {
                    emit(Resource.Error(result.message ?: "Erro ao carregar fatura"))
                    return@flow
                }
            }
            when (val result = transactionRepository.getInvoicePayments(FinancialTotals.monthKey(target))) {
                is Resource.Success -> payments += result.data.orEmpty().map { it.toModel() }
                else -> {
                    emit(Resource.Error(result.message ?: "Erro ao carregar pagamentos de fatura"))
                    return@flow
                }
            }
        }

        emit(Resource.Success(build(month, creditCards, projections, payments)))
    }

    companion object {
        fun paidMonthOf(card: CardDto, month: YearMonth): YearMonth =
            FinancialTotals.paidInvoiceMonth(card.closingDay, card.invoiceDueDate, month)

        /**
         * Monta as faturas pagas em [month] a partir das projeções já carregadas (indexadas pelo
         * `invoiceMonth`) e dos pagamentos encontrados para esses meses.
         */
        fun build(
            month: YearMonth,
            creditCards: List<CardDto>,
            projections: Map<YearMonth, InvoiceProjection>,
            payments: List<Transaction>,
        ): List<PaidInvoice> {
            val fallbackMonth = month.minusMonths(1)
            val knownIds = creditCards.map { it.id }.toSet()

            fun itemsOf(target: YearMonth): List<Transaction> {
                val projection = projections[target] ?: return emptyList()
                return FinancialTotals.invoiceItems(
                    invoiceMonth = target,
                    invoiceTransactions = projection.invoiceTransactions,
                    recurringTransactions = projection.recurringTransactions,
                )
            }

            fun paymentFor(cardId: String?, target: YearMonth): Transaction? {
                val key = FinancialTotals.monthKey(target)
                return payments.firstOrNull {
                    it.invoicePaymentMonth == key && (if (cardId == null) it.cardId !in knownIds else it.cardId == cardId)
                }
            }

            val invoices = creditCards.map { card ->
                val target = paidMonthOf(card, month)
                PaidInvoice(
                    cardId = card.id,
                    bank = card.bank,
                    closingDay = card.closingDay,
                    invoiceDueDate = card.invoiceDueDate,
                    invoiceMonth = target,
                    items = itemsOf(target).filter { it.cardId == card.id },
                    payment = paymentFor(card.id, target),
                )
            }

            // Compras de cartões que não existem mais (ou sem CREDIT) mantêm o comportamento antigo (M-1)
            val orphanItems = itemsOf(fallbackMonth).filter { it.cardId !in knownIds }
            val orphanInvoice = orphanItems.takeIf { it.isNotEmpty() }?.let {
                PaidInvoice(
                    cardId = "",
                    bank = "Outros cartões",
                    closingDay = 0,
                    invoiceDueDate = 0,
                    invoiceMonth = fallbackMonth,
                    items = it,
                    payment = paymentFor(null, fallbackMonth),
                )
            }

            return (invoices + listOfNotNull(orphanInvoice))
                .filter { it.items.isNotEmpty() || it.isPaid }
        }
    }
}
