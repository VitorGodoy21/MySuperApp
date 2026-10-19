package com.vfdeginformatica.mysuperapp.presentation.screen.card_invoice.contract

import com.google.firebase.Timestamp
import com.vfdeginformatica.mysuperapp.domain.model.Transaction
import com.vfdeginformatica.mysuperapp.presentation.screen.new_transaction.contract.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.YearMonth

class CardInvoiceUiStateTest {

    private fun tx(
        id: String,
        amount: Double,
        cardId: String = "c1",
        type: TransactionType = TransactionType.EXPENSE,
        paymentMethod: String = "CREDIT",
        isRecurring: Boolean = false,
        installments: Int? = null,
        installmentNumber: Int? = null,
        invoiceMonth: String? = "2026-10",
        epochSeconds: Long? = null
    ) = Transaction(
        id = id,
        title = id,
        description = "",
        type = type,
        paymentMethod = paymentMethod,
        amount = amount,
        isRecurring = isRecurring,
        date = epochSeconds?.let { Timestamp(it, 0) },
        categoryIds = emptyList(),
        status = "pending",
        cardId = cardId,
        installments = installments,
        installmentNumber = installmentNumber,
        installmentGroupId = installments?.let { "g-$id" },
        invoiceMonth = invoiceMonth
    )

    private val base = CardInvoiceUiState(cardId = "c1", invoiceMonth = YearMonth.of(2026, 10))

    @Test
    fun `items only include the selected card`() {
        val state = base.copy(
            invoiceTransactions = listOf(
                tx("tv", 300.0, cardId = "c1"),
                tx("other", 50.0, cardId = "c2")
            ),
            recurringTransactions = listOf(
                tx("netflix", 40.0, cardId = "c1", isRecurring = true, invoiceMonth = "2026-01"),
                tx("spotify", 20.0, cardId = "c2", isRecurring = true, invoiceMonth = "2026-01")
            )
        )
        assertEquals(listOf("tv", "netflix"), state.items.map { it.id })
        assertEquals(340.0, state.total, 0.001)
    }

    @Test
    fun `recurring already returned by the invoice query is not duplicated`() {
        val state = base.copy(
            invoiceTransactions = listOf(tx("netflix", 40.0, isRecurring = true)),
            recurringTransactions = listOf(
                tx("netflix", 40.0, isRecurring = true, invoiceMonth = "2026-10"),
                tx("rent", 1500.0, isRecurring = true, paymentMethod = "DEBIT", invoiceMonth = null),
                tx("cashback", 10.0, type = TransactionType.INCOME, isRecurring = true, invoiceMonth = null)
            )
        )
        assertEquals(listOf("netflix"), state.items.map { it.id })
        assertEquals(40.0, state.total, 0.001)
    }

    @Test
    fun `items are sorted by date desc with recurring at the end`() {
        val state = base.copy(
            invoiceTransactions = listOf(
                tx("old", 10.0, epochSeconds = 1_000),
                tx("newest", 30.0, epochSeconds = 3_000),
                tx("mid", 20.0, epochSeconds = 2_000)
            ),
            recurringTransactions = listOf(
                tx("sub", 5.0, isRecurring = true, invoiceMonth = "2026-01", epochSeconds = 9_000)
            )
        )
        assertEquals(listOf("newest", "mid", "old", "sub"), state.items.map { it.id })
    }

    @Test
    fun `installment purchase total and subtotals`() {
        val installment = tx("tv", 300.0, installments = 10, installmentNumber = 3)
        val single = tx("shoes", 200.0)
        val recurring = tx("netflix", 40.0, isRecurring = true, invoiceMonth = "2026-01")
        val state = base.copy(
            invoiceTransactions = listOf(installment, single),
            recurringTransactions = listOf(recurring)
        )

        assertEquals(3000.0, installment.installmentPurchaseTotal!!, 0.001)
        assertNull(single.installmentPurchaseTotal)
        assertEquals(300.0, state.installmentsTotal, 0.001)
        assertEquals(200.0, state.singleTotal, 0.001)
        assertEquals(40.0, state.recurringTotal, 0.001)
        assertEquals(540.0, state.total, 0.001)
    }
}
