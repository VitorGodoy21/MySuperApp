package com.vfdeginformatica.mysuperapp.domain.use_case.financial.invoice

import com.google.firebase.Timestamp
import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.data.remote.dto.CardDto
import com.vfdeginformatica.mysuperapp.data.remote.dto.TransactionDto
import com.vfdeginformatica.mysuperapp.domain.model.FinancialTotals
import com.vfdeginformatica.mysuperapp.domain.model.PaidInvoice
import com.vfdeginformatica.mysuperapp.domain.repository.CardRepository
import com.vfdeginformatica.mysuperapp.domain.repository.FakeTransactionRepository
import com.vfdeginformatica.mysuperapp.domain.use_case.financial.transaction.GetInvoiceProjectionUseCase
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.YearMonth

class GetPaidInvoicesUseCaseTest {

    private class Cards(private val cards: List<CardDto>?) : CardRepository {
        override suspend fun getCards(): List<CardDto>? = cards
        override suspend fun addCard(card: CardDto): Resource<String> = Resource.Success("id")
        override suspend fun updateCard(
            cardId: String, bank: String, paymentMethods: List<String>,
            finalCode: String, closingDay: Int, invoiceDueDate: Int
        ): Resource<Unit> = Resource.Success(Unit)
    }

    private val itau = CardDto(
        id = "itau", bank = "Itaú", paymentMethods = listOf("CREDIT", "DEBIT"), closingDay = 6, invoiceDueDate = 12
    )
    private val nubank = CardDto(
        id = "nu", bank = "Nubank", paymentMethods = listOf("CREDIT"), closingDay = 25, invoiceDueDate = 5
    )
    private val debitOnly = CardDto(id = "inter", bank = "Inter", paymentMethods = listOf("DEBIT"))

    private val nov = YearMonth.of(2026, 11)

    private fun credit(
        id: String, amount: Double, cardId: String, invoiceMonth: String?,
        isRecurring: Boolean = false, recurrenceStart: String? = null
    ) = TransactionDto(
        id = id, title = id, transactionType = "EXPENSE", paymentMethod = "CREDIT", amount = amount,
        cardId = cardId, invoiceMonth = invoiceMonth, isRecurring = isRecurring, recurrenceStart = recurrenceStart,
        transactionDate = Timestamp.now()
    )

    private fun payment(id: String, amount: Double, cardId: String, invoicePaymentMonth: String) = TransactionDto(
        id = id, title = id, transactionType = "EXPENSE", paymentMethod = FinancialTotals.INVOICE_PAYMENT,
        amount = amount, cardId = cardId, invoicePaymentMonth = invoicePaymentMonth, transactionDate = Timestamp.now()
    )

    private suspend fun run(cards: List<CardDto>?, repo: FakeTransactionRepository): Resource<List<PaidInvoice>> =
        GetPaidInvoicesUseCase(Cards(cards), repo, GetInvoiceProjectionUseCase(repo))(nov).toList().last()

    @Test
    fun `each credit card points to the invoice paid in the month according to its own rule`() = runTest {
        val repo = FakeTransactionRepository().apply {
            invoiceTransactions = listOf(
                credit("itau-oct", 100.0, "itau", "2026-10"),
                credit("itau-nov", 4758.91, "itau", "2026-11"),
                credit("nu-oct", 200.0, "nu", "2026-10"),
                credit("nu-nov", 300.0, "nu", "2026-11"),
            )
        }

        val result = run(listOf(itau, nubank, debitOnly), repo)

        assertTrue(result is Resource.Success)
        val byCard = result.data!!.associateBy { it.cardId }
        assertEquals(setOf("itau", "nu"), byCard.keys)
        // Itaú fecha 6 / vence 12 ⇒ a fatura de novembro é paga em novembro
        assertEquals(nov, byCard["itau"]!!.invoiceMonth)
        assertEquals(listOf("itau-nov"), byCard["itau"]!!.items.map { it.id })
        assertEquals(4758.91, byCard["itau"]!!.projectedTotal, 0.001)
        // Nubank fecha 25 / vence 5 ⇒ em novembro paga a fatura de outubro
        assertEquals(nov.minusMonths(1), byCard["nu"]!!.invoiceMonth)
        assertEquals(listOf("nu-oct"), byCard["nu"]!!.items.map { it.id })
        assertFalse(byCard.values.any { it.isPaid })
    }

    @Test
    fun `recurring credit expenses enter once per invoice`() = runTest {
        val stream = credit("stream", 50.0, "itau", invoiceMonth = "2026-11", isRecurring = true, recurrenceStart = "2026-01")
        val repo = FakeTransactionRepository().apply {
            // "stream" já tem invoiceMonth gravado, então aparece nas duas consultas
            invoiceTransactions = listOf(credit("tv", 100.0, "itau", "2026-11"), stream)
            recurringTransactions = listOf(
                stream,
                credit("gym", 80.0, "itau", invoiceMonth = null, isRecurring = true, recurrenceStart = "2026-01"),
            )
        }

        val invoice = run(listOf(itau), repo).data!!.single()

        // "stream" já veio na consulta por invoiceMonth; só "gym" é materializado da recorrência
        assertEquals(1, invoice.items.count { it.id == "stream" })
        assertEquals(1, invoice.items.count { it.id == "gym" })
        assertEquals(230.0, invoice.projectedTotal, 0.001)
    }

    @Test
    fun `payment is matched by card and invoice month and replaces the projected total`() = runTest {
        val repo = FakeTransactionRepository().apply {
            invoiceTransactions = listOf(
                credit("itau-nov", 500.0, "itau", "2026-11"),
                credit("nu-oct", 200.0, "nu", "2026-10"),
            )
            invoicePayments = listOf(
                payment("pay-itau", 480.0, "itau", "2026-11"),
                payment("pay-nu-old", 999.0, "nu", "2026-09"), // outra fatura, ignorado
            )
        }

        val byCard = run(listOf(itau, nubank), repo).data!!.associateBy { it.cardId }

        assertTrue(byCard["itau"]!!.isPaid)
        assertEquals("pay-itau", byCard["itau"]!!.payment?.id)
        assertEquals(480.0, byCard["itau"]!!.effectiveTotal, 0.001)
        assertNull(byCard["nu"]!!.payment)
        assertEquals(680.0, FinancialTotals.paidInvoiceTotal(byCard.values.toList()), 0.001)
    }

    @Test
    fun `items of unknown cards fall back to previous-month group`() = runTest {
        val repo = FakeTransactionRepository().apply {
            invoiceTransactions = listOf(
                credit("ghost-oct", 70.0, "deleted", "2026-10"),
                credit("ghost-nov", 30.0, "deleted", "2026-11"),
            )
        }

        val invoices = run(listOf(itau), repo).data!!

        val orphan = invoices.single()
        assertEquals("", orphan.cardId)
        assertEquals(nov.minusMonths(1), orphan.invoiceMonth)
        assertEquals(listOf("ghost-oct"), orphan.items.map { it.id })
    }

    @Test
    fun `card error is surfaced`() = runTest {
        val result = run(null, FakeTransactionRepository())
        assertTrue(result is Resource.Error)
    }
}
