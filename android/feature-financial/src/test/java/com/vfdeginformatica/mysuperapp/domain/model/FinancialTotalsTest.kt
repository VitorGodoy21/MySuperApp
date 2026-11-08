package com.vfdeginformatica.mysuperapp.domain.model

import com.vfdeginformatica.mysuperapp.domain.model.FinancialTotals.debitExpenseTotal
import com.vfdeginformatica.mysuperapp.domain.model.FinancialTotals.expenses
import com.vfdeginformatica.mysuperapp.presentation.screen.new_transaction.contract.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.YearMonth

class FinancialTotalsTest {

    private fun tx(id: String, amount: Double, paymentMethod: String = "DEBIT") = Transaction(
        id = id, title = id, description = "", type = TransactionType.EXPENSE, paymentMethod = paymentMethod,
        amount = amount, isRecurring = false, date = null, categoryIds = emptyList(), status = "pending",
        cardId = "c1", installments = null, installmentNumber = null, installmentGroupId = null, invoiceMonth = null
    )

    private val nov = YearMonth.of(2026, 11)

    @Test
    fun `invoice due after closing in the same month is paid in that month`() {
        assertEquals(nov, FinancialTotals.paidInvoiceMonth(closingDay = 6, invoiceDueDate = 12, month = nov))
        assertEquals(nov, FinancialTotals.paymentMonthOf(closingDay = 6, invoiceDueDate = 12, invoiceMonth = nov))
    }

    @Test
    fun `invoice due in the following month is paid one month later`() {
        assertEquals(nov.minusMonths(1), FinancialTotals.paidInvoiceMonth(25, 5, nov))
        assertEquals(nov.plusMonths(1), FinancialTotals.paymentMonthOf(25, 5, nov))
    }

    @Test
    fun `card without closing or due day keeps previous-month behaviour`() {
        assertEquals(nov.minusMonths(1), FinancialTotals.paidInvoiceMonth(0, 0, nov))
        assertEquals(nov.minusMonths(1), FinancialTotals.paidInvoiceMonth(6, 0, nov))
        assertEquals(nov.minusMonths(1), FinancialTotals.paidInvoiceMonth(0, 12, nov))
    }

    @Test
    fun `invoice payments are not purchases`() {
        val list = listOf(tx("market", 100.0), tx("pay", 900.0, paymentMethod = FinancialTotals.INVOICE_PAYMENT))
        assertEquals(listOf("market"), list.expenses().map { it.id })
        assertEquals(100.0, list.debitExpenseTotal(), 0.001)
        assertEquals(
            emptyList<Transaction>(),
            FinancialTotals.invoiceItems(nov, listOf(list[1]), emptyList())
        )
    }

    @Test
    fun `paid invoice total uses the paid amount when a payment exists`() {
        val open = PaidInvoice("c1", "Itaú", 6, 12, nov, items = listOf(tx("a", 300.0, "CREDIT")))
        val paid = open.copy(cardId = "c2", payment = tx("pay", 250.0, FinancialTotals.INVOICE_PAYMENT))
        assertEquals(300.0, open.effectiveTotal, 0.001)
        assertEquals(250.0, paid.effectiveTotal, 0.001)
        assertEquals(550.0, FinancialTotals.paidInvoiceTotal(listOf(open, paid)), 0.001)
    }
}
