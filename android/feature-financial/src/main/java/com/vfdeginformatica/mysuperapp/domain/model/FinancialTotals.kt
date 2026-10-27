package com.vfdeginformatica.mysuperapp.domain.model

import com.vfdeginformatica.mysuperapp.presentation.screen.new_transaction.contract.TransactionType
import java.time.YearMonth

/**
 * Pure month-total formulas shared by the financial dashboard ([FinancialUiState]) and the
 * home summary ([GetMonthFinancialSummaryUseCase]) so both screens agree on the numbers.
 */
object FinancialTotals {
    const val CREDIT = "CREDIT"

    fun monthKey(month: YearMonth): String = "%04d-%02d".format(month.year, month.monthValue)

    fun List<Transaction>.expenses(): List<Transaction> = filter { it.type == TransactionType.EXPENSE }

    fun List<Transaction>.incomeTotal(): Double =
        filter { it.type == TransactionType.INCOME }.sumOf { it.amount }

    fun List<Transaction>.creditExpenseTotal(): Double =
        expenses().filter { it.paymentMethod == CREDIT }.sumOf { it.amount }

    fun List<Transaction>.debitExpenseTotal(): Double =
        expenses().filter { it.paymentMethod != CREDIT }.sumOf { it.amount }

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
        return invoiceTransactions + recurringTransactions.filter {
            it.type == TransactionType.EXPENSE && it.paymentMethod == CREDIT && it.invoiceMonth != key
        }
    }

    /** Cash that leaves the account in the month: debit purchases + the invoice paid this month. */
    fun cashExpense(debitExpense: Double, paidInvoiceTotal: Double): Double = debitExpense + paidInvoiceTotal

    fun balance(totalIncome: Double, cashExpense: Double): Double = totalIncome - cashExpense
}

/** Month totals shown on the Home finance card. */
data class MonthFinancialSummary(
    val month: YearMonth,
    val totalIncome: Double,
    val cashExpense: Double,
) {
    val balance: Double get() = FinancialTotals.balance(totalIncome, cashExpense)
}
