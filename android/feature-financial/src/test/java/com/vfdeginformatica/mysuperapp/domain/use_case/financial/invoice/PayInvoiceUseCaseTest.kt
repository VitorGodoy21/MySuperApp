package com.vfdeginformatica.mysuperapp.domain.use_case.financial.invoice

import com.google.firebase.Timestamp
import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.domain.model.FinancialTotals
import com.vfdeginformatica.mysuperapp.domain.model.PaidInvoice
import com.vfdeginformatica.mysuperapp.domain.model.Transaction
import com.vfdeginformatica.mysuperapp.domain.repository.FakeTransactionRepository
import com.vfdeginformatica.mysuperapp.presentation.screen.new_transaction.contract.TransactionType
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.YearMonth

class PayInvoiceUseCaseTest {

    private fun tx(id: String, amount: Double, paymentMethod: String = "CREDIT") = Transaction(
        id = id, title = id, description = "", type = TransactionType.EXPENSE, paymentMethod = paymentMethod,
        amount = amount, isRecurring = false, date = null, categoryIds = emptyList(), status = "pending",
        cardId = "itau", installments = null, installmentNumber = null, installmentGroupId = null, invoiceMonth = null
    )

    private val invoice = PaidInvoice(
        cardId = "itau", bank = "Itaú", closingDay = 6, invoiceDueDate = 12,
        invoiceMonth = YearMonth.of(2026, 11), items = listOf(tx("tv", 500.0))
    )

    @Test
    fun `creates an INVOICE_PAYMENT expense tied to the invoice month`() = runTest {
        val repo = FakeTransactionRepository()
        val date = Timestamp.now()

        val result = PayInvoiceUseCase(repo)(invoice, 480.0, date).toList().last()

        assertTrue(result is Resource.Success)
        val dto = repo.added.single()
        assertEquals("EXPENSE", dto.transactionType)
        assertEquals(FinancialTotals.INVOICE_PAYMENT, dto.paymentMethod)
        assertEquals("itau", dto.cardId)
        assertEquals(480.0, dto.amount, 0.001)
        assertEquals("2026-11", dto.invoicePaymentMonth)
        assertNull(dto.invoiceMonth)
        assertEquals("Pagamento da fatura — Itaú", dto.title)
        assertEquals(date, dto.transactionDate)
        assertTrue(!dto.isRecurring)
    }

    @Test
    fun `rejects zero amount, already paid and orphan invoices`() = runTest {
        val repo = FakeTransactionRepository()
        val useCase = PayInvoiceUseCase(repo)

        assertTrue(useCase(invoice, 0.0, Timestamp.now()).toList().last() is Resource.Error)
        val paid = invoice.copy(payment = tx("pay", 500.0, FinancialTotals.INVOICE_PAYMENT))
        assertTrue(useCase(paid, 100.0, Timestamp.now()).toList().last() is Resource.Error)
        assertTrue(useCase(invoice.copy(cardId = ""), 100.0, Timestamp.now()).toList().last() is Resource.Error)
        assertTrue(repo.added.isEmpty())
    }
}
