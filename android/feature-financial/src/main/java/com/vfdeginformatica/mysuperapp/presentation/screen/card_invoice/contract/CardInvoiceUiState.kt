package com.vfdeginformatica.mysuperapp.presentation.screen.card_invoice.contract

import com.vfdeginformatica.mysuperapp.data.remote.dto.CardDto
import com.vfdeginformatica.mysuperapp.data.remote.dto.CategoryTransactionDto
import com.vfdeginformatica.mysuperapp.domain.model.FinancialTotals
import com.vfdeginformatica.mysuperapp.domain.model.PaidInvoice
import com.vfdeginformatica.mysuperapp.domain.model.Transaction
import com.vfdeginformatica.mysuperapp.presentation.screen.financial.contract.PendingRecurringAction
import com.vfdeginformatica.mysuperapp.presentation.screen.new_transaction.contract.TransactionType
import java.time.YearMonth

data class CardInvoiceUiState(
    val cardId: String = "",
    val invoiceMonth: YearMonth = YearMonth.now(),
    val card: CardDto? = null,
    val availableCategories: List<CategoryTransactionDto> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    // Resultado de GetInvoiceProjectionUseCase(invoiceMonth), ainda sem filtro de cartão
    val invoiceTransactions: List<Transaction> = emptyList(),
    val recurringTransactions: List<Transaction> = emptyList(),
    // Pagamento registrado para esta fatura (null = em aberto)
    val payment: Transaction? = null,
    val isPayDialogOpen: Boolean = false,
    val isPaying: Boolean = false,
    // Edição (dialog)
    val editingTransaction: Transaction? = null,
    val isSavingEdit: Boolean = false,
    val pendingRecurringAction: PendingRecurringAction? = null
) {
    companion object {
        const val CREDIT = "CREDIT"
    }

    val invoiceMonthKey: String
        get() = "%04d-%02d".format(invoiceMonth.year, invoiceMonth.monthValue)

    /**
     * Lançamentos da fatura deste cartão: compras/parcelas com `invoiceMonth` igual ao mês mais
     * recorrentes de crédito (sem duplicar as que já vieram pela query), ordenados por data da
     * compra (mais recente primeiro) com as recorrentes ao final.
     */
    val items: List<Transaction>
        get() {
            val purchases = invoiceTransactions
                .filter { it.cardId == cardId }
                .sortedByDescending { it.date?.seconds ?: Long.MIN_VALUE }
            val recurring = recurringTransactions.filter {
                it.cardId == cardId &&
                        it.type == TransactionType.EXPENSE &&
                        it.paymentMethod == CREDIT &&
                        it.invoiceMonth != invoiceMonthKey
            }
            return purchases + recurring
        }

    val total: Double
        get() = items.sumOf { it.amount }

    val installmentsTotal: Double
        get() = items.filter { it.isInstallment }.sumOf { it.amount }

    val recurringTotal: Double
        get() = items.filter { it.isRecurring }.sumOf { it.amount }

    val singleTotal: Double
        get() = items.filter { !it.isInstallment && !it.isRecurring }.sumOf { it.amount }

    val isPaid: Boolean get() = payment != null

    /** Mês em que esta fatura sai da conta (regra fecha/vence do cartão). */
    val paymentMonth: YearMonth
        get() = FinancialTotals.paymentMonthOf(card?.closingDay ?: 0, card?.invoiceDueDate ?: 0, invoiceMonth)

    /** Fatura deste cartão no formato usado pelo dialog "Pagar fatura" e pelo [PayInvoiceUseCase]. */
    val paidInvoice: PaidInvoice
        get() = PaidInvoice(
            cardId = cardId,
            bank = card?.bank ?: "Cartão",
            closingDay = card?.closingDay ?: 0,
            invoiceDueDate = card?.invoiceDueDate ?: 0,
            invoiceMonth = invoiceMonth,
            items = items,
            payment = payment,
        )
}

val Transaction.isInstallment: Boolean
    get() = (installments ?: 0) > 1

/**
 * Valor total aproximado da compra parcelada (parcela × nº de parcelas). Pode divergir
 * alguns centavos do valor real, pois a última parcela absorve o arredondamento.
 */
val Transaction.installmentPurchaseTotal: Double?
    get() = installments?.takeIf { it > 1 }?.let { amount * it }
