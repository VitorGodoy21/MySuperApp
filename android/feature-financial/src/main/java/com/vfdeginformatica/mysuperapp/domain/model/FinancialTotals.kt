package com.vfdeginformatica.mysuperapp.domain.model

import com.vfdeginformatica.mysuperapp.presentation.screen.new_transaction.contract.TransactionType
import java.time.YearMonth

/**
 * Pure month-total formulas shared by the financial dashboard ([FinancialUiState]) and the
 * home summary ([GetMonthFinancialSummaryUseCase]) so both screens agree on the numbers.
 */
object FinancialTotals {
    const val CREDIT = "CREDIT"

    /** `paymentMethod` da transação que quita uma fatura de cartão (não é uma compra). */
    const val INVOICE_PAYMENT = "INVOICE_PAYMENT"

    fun monthKey(month: YearMonth): String = "%04d-%02d".format(month.year, month.monthValue)

    val Transaction.isInvoicePayment: Boolean
        get() = paymentMethod == INVOICE_PAYMENT

    /** Despesas que são compras (exclui pagamentos de fatura, que já são representados pela fatura). */
    fun List<Transaction>.expenses(): List<Transaction> =
        filter { it.type == TransactionType.EXPENSE && !it.isInvoicePayment }

    fun List<Transaction>.incomeTotal(): Double =
        filter { it.type == TransactionType.INCOME }.sumOf { it.amount }

    fun List<Transaction>.creditExpenseTotal(): Double =
        expenses().filter { it.paymentMethod == CREDIT }.sumOf { it.amount }

    fun List<Transaction>.debitExpenseTotal(): Double =
        expenses().filter { it.paymentMethod != CREDIT }.sumOf { it.amount }

    /**
     * Mês da fatura (`invoiceMonth`) que é paga em [month] para um cartão que fecha em
     * [closingDay] e vence em [invoiceDueDate]. Quando o vencimento cai depois do fechamento
     * no mesmo mês (ex.: fecha dia 6, vence dia 12) a fatura `M` é paga no próprio `M`; caso
     * contrário (vence no mês seguinte, ou cartão sem fechamento/vencimento configurado) a
     * fatura paga em `M` é a que fechou em `M-1`.
     */
    fun paidInvoiceMonth(closingDay: Int, invoiceDueDate: Int, month: YearMonth): YearMonth =
        if (dueAfterClosingSameMonth(closingDay, invoiceDueDate)) month else month.minusMonths(1)

    /** Inverso de [paidInvoiceMonth]: mês em que a fatura [invoiceMonth] é paga. */
    fun paymentMonthOf(closingDay: Int, invoiceDueDate: Int, invoiceMonth: YearMonth): YearMonth =
        if (dueAfterClosingSameMonth(closingDay, invoiceDueDate)) invoiceMonth else invoiceMonth.plusMonths(1)

    private fun dueAfterClosingSameMonth(closingDay: Int, invoiceDueDate: Int): Boolean =
        closingDay in 1..31 && invoiceDueDate in 1..31 && invoiceDueDate > closingDay

    /**
     * Items of the invoice closed in [invoiceMonth]: purchases/installments from the
     * `invoiceMonth` query plus recurring credit expenses, without duplicating the ones
     * whose own `invoiceMonth` already equals that month.
     */
    fun invoiceItems(
        invoiceMonth: YearMonth,
        invoiceTransactions: List<Transaction>,
        recurringTransactions: List<Transaction>,
    ): List<Transaction> {
        val key = monthKey(invoiceMonth)
        return invoiceTransactions.filterNot { it.isInvoicePayment } + recurringTransactions.filter {
            it.type == TransactionType.EXPENSE && it.paymentMethod == CREDIT && it.invoiceMonth != key
        }
    }

    /** Total que sai da conta pelas faturas do mês: valor pago quando houver, senão o projetado. */
    fun paidInvoiceTotal(invoices: List<PaidInvoice>): Double = invoices.sumOf { it.effectiveTotal }

    /** Cash that leaves the account in the month: debit purchases + the invoices paid this month. */
    fun cashExpense(debitExpense: Double, paidInvoiceTotal: Double): Double = debitExpense + paidInvoiceTotal

    fun balance(totalIncome: Double, cashExpense: Double): Double = totalIncome - cashExpense
}

/**
 * Fatura de um cartão que é paga no mês selecionado: itens projetados e, quando já quitada,
 * a transação de pagamento (cujo valor substitui o projetado no saldo de caixa).
 */
data class PaidInvoice(
    val cardId: String,
    val bank: String,
    val closingDay: Int,
    val invoiceDueDate: Int,
    val invoiceMonth: YearMonth,
    val items: List<Transaction>,
    val payment: Transaction? = null,
) {
    val invoiceMonthKey: String get() = FinancialTotals.monthKey(invoiceMonth)
    val projectedTotal: Double get() = items.sumOf { it.amount }
    val isPaid: Boolean get() = payment != null
    val effectiveTotal: Double get() = payment?.amount ?: projectedTotal
}

/** Month totals shown on the Home finance card. */
data class MonthFinancialSummary(
    val month: YearMonth,
    val totalIncome: Double,
    val cashExpense: Double,
) {
    val balance: Double get() = FinancialTotals.balance(totalIncome, cashExpense)
}
