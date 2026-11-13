package com.vfdeginformatica.mysuperapp.presentation.screen.financial.contract

import com.google.firebase.Timestamp
import com.vfdeginformatica.mysuperapp.data.remote.dto.CategoryTransactionDto
import com.vfdeginformatica.mysuperapp.domain.model.PaidInvoice
import com.vfdeginformatica.mysuperapp.domain.model.Transaction
import com.vfdeginformatica.mysuperapp.presentation.screen.new_transaction.contract.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.YearMonth

class FinancialUiStateTest {

    private fun tx(
        id: String,
        amount: Double,
        type: TransactionType = TransactionType.EXPENSE,
        paymentMethod: String = "DEBIT",
        isRecurring: Boolean = false,
        installments: Int? = null,
        installmentGroupId: String? = null,
        categoryIds: List<String> = emptyList(),
        cardId: String = "",
        invoiceMonth: String? = null
    ) = Transaction(
        id = id,
        title = id,
        description = "",
        type = type,
        paymentMethod = paymentMethod,
        amount = amount,
        isRecurring = isRecurring,
        date = null,
        categoryIds = categoryIds,
        status = "pending",
        cardId = cardId,
        installments = installments,
        installmentNumber = null,
        installmentGroupId = installmentGroupId,
        invoiceMonth = invoiceMonth
    )

    private val month = YearMonth.of(2026, 10)

    private val sample = listOf(
        tx("salary", 5000.0, type = TransactionType.INCOME, cardId = "c1", isRecurring = true),
        tx("netflix", 50.0, paymentMethod = "CREDIT", isRecurring = true, categoryIds = listOf("stream"), cardId = "c1"),
        tx("tv", 300.0, paymentMethod = "CREDIT", installments = 10, installmentGroupId = "g1", categoryIds = listOf("home"), cardId = "c1"),
        tx("market", 400.0, paymentMethod = "DEBIT", categoryIds = listOf("food", "home"), cardId = "c2"),
        tx("cash", 100.0, paymentMethod = "DEBIT")
    )

    private val base = FinancialUiState(selectedMonth = month, transactions = sample)

    // ---- Resumo do mês ----

    @Test
    fun `month totals ignore filters`() {
        val state = base.copy(typeFilter = TransactionTypeFilter.INCOME, recurringOnly = true)
        assertEquals(5000.0, state.totalIncome, 0.001)
        assertEquals(850.0, state.totalExpense, 0.001)
        assertEquals(4500.0, state.balance, 0.001)
    }

    // ---- Saldo de caixa (débito + faturas pagas no mês) ----

    private fun invoice(
        cardId: String,
        invoiceMonth: YearMonth,
        items: List<Transaction>,
        payment: Transaction? = null,
        closingDay: Int = 6,
        invoiceDueDate: Int = 12
    ) = PaidInvoice(
        cardId = cardId, bank = cardId, closingDay = closingDay, invoiceDueDate = invoiceDueDate,
        invoiceMonth = invoiceMonth, items = items, payment = payment
    )

    @Test
    fun `balance ignores credit purchases of the month`() {
        assertEquals(500.0, base.cashExpense, 0.001)
        assertEquals(4500.0, base.balance, 0.001)
        assertEquals(0.0, base.paidInvoiceTotal, 0.001)
    }

    @Test
    fun `balance includes the invoices paid in the month`() {
        val state = base.copy(
            paidInvoices = listOf(
                invoice(
                    "c1", YearMonth.of(2026, 10),
                    listOf(
                        tx("tv-prev", 300.0, paymentMethod = "CREDIT", cardId = "c1", invoiceMonth = "2026-10"),
                        tx("shoes", 200.0, paymentMethod = "CREDIT", cardId = "c1", invoiceMonth = "2026-10")
                    )
                )
            )
        )
        assertEquals(500.0, state.paidInvoiceTotal, 0.001)
        assertEquals(1000.0, state.cashExpense, 0.001)
        assertEquals(4000.0, state.balance, 0.001)
        assertEquals(5000.0, state.totalIncome, 0.001)
        assertEquals(500.0, state.debitExpense, 0.001)
        assertEquals("c1", state.paidInvoiceFor("c1")?.cardId)
    }

    @Test
    fun `paid amount replaces projected total and payment is not counted as purchase`() {
        val payment = tx("pay", 450.0, paymentMethod = "INVOICE_PAYMENT", cardId = "c1")
        val state = base.copy(
            transactions = sample + payment,
            paidInvoices = listOf(
                invoice(
                    "c1", YearMonth.of(2026, 10),
                    listOf(tx("tv-prev", 500.0, paymentMethod = "CREDIT", cardId = "c1", invoiceMonth = "2026-10")),
                    payment = payment
                )
            )
        )
        assertTrue(state.paidInvoices.single().isPaid)
        assertEquals(450.0, state.paidInvoiceTotal, 0.001)
        assertEquals(500.0 + 450.0, state.cashExpense, 0.001)
        // totais de compras não mudam com o pagamento
        assertEquals(850.0, state.totalExpense, 0.001)
        assertEquals(500.0, state.debitExpense, 0.001)
        assertEquals(350.0, state.creditExpense, 0.001)
    }

    @Test
    fun `debit filter does not include invoice payments`() {
        val payment = tx("pay", 450.0, paymentMethod = "INVOICE_PAYMENT", cardId = "c1")
        val state = base.copy(transactions = sample + payment, paymentMethodFilter = PaymentMethodFilter.DEBIT)
        assertFalse(state.filteredTransactions.any { it.id == "pay" })
        val all = base.copy(transactions = sample + payment)
        assertTrue(all.filteredTransactions.any { it.id == "pay" })
    }

    @Test
    fun `credit and debit breakdown`() {
        assertEquals(350.0, base.creditExpense, 0.001)
        assertEquals(500.0, base.debitExpense, 0.001)
    }

    @Test
    fun `expense nature breakdown`() {
        assertEquals(50.0, base.recurringExpense, 0.001)
        assertEquals(300.0, base.installmentExpense, 0.001)
        assertEquals(500.0, base.singleExpense, 0.001)
    }

    @Test
    fun `expense by category counts multi-category in each and no-category as null`() {
        val byCat = base.expenseByCategory.associateBy { it.categoryId }
        assertEquals(700.0, byCat["home"]!!.total, 0.001)
        assertEquals(400.0, byCat["food"]!!.total, 0.001)
        assertEquals(50.0, byCat["stream"]!!.total, 0.001)
        assertEquals(100.0, byCat[null]!!.total, 0.001)
        assertEquals(700.0 / 850.0 * 100, byCat["home"]!!.percent, 0.001)
        assertEquals("home", base.expenseByCategory.first().categoryId)
    }

    @Test
    fun `expense by category aggregates beyond top into Outras`() {
        val many = (1..7).map { tx("t$it", it.toDouble(), categoryIds = listOf("cat$it")) }
        val result = FinancialUiState(transactions = many).expenseByCategory
        assertEquals(FinancialUiState.TOP_CATEGORIES + 1, result.size)
        val others = result.last()
        assertEquals(OTHERS_KEY, others.categoryId)
        // cat1 e cat2 (as menores) viram "Outras"
        assertEquals(3.0, others.total, 0.001)
    }

    @Test
    fun `expense by category is empty without expenses`() {
        val state = FinancialUiState(transactions = listOf(tx("s", 10.0, type = TransactionType.INCOME)))
        assertTrue(state.expenseByCategory.isEmpty())
    }

    @Test
    fun `expense by card sorted descending and ignores empty card`() {
        val byCard = base.expenseByCard.entries.toList()
        assertEquals(listOf("c2", "c1"), byCard.map { it.key })
        assertEquals(400.0, byCard[0].value, 0.001)
        assertEquals(350.0, byCard[1].value, 0.001)
    }

    @Test
    fun `invoice by card dedups recurring credit already in invoice month`() {
        val state = base.copy(
            invoiceTransactions = listOf(
                tx("tv-3", 30.0, paymentMethod = "CREDIT", cardId = "c1", invoiceMonth = "2026-11"),
                tx("netflix", 50.0, paymentMethod = "CREDIT", isRecurring = true, cardId = "c1", invoiceMonth = "2026-11")
            ),
            recurringTransactions = listOf(
                tx("netflix", 50.0, paymentMethod = "CREDIT", isRecurring = true, cardId = "c1", invoiceMonth = "2026-11"),
                tx("spotify", 20.0, paymentMethod = "CREDIT", isRecurring = true, cardId = "c2", invoiceMonth = "2026-09"),
                tx("gym", 90.0, paymentMethod = "DEBIT", isRecurring = true, cardId = "c2")
            )
        )
        assertEquals(80.0, state.invoiceByCard["c1"]!!, 0.001)
        assertEquals(20.0, state.invoiceByCard["c2"]!!, 0.001)
        assertEquals(100.0, state.projectedInvoiceTotal, 0.001)
    }

    // ---- Filtros ----

    @Test
    fun `filtered transactions are sorted by date descending`() {
        val state = base.copy(
            transactions = listOf(
                tx("old", 1.0).copy(date = Timestamp(1_000, 0)),
                tx("undated", 1.0),
                tx("newest", 1.0).copy(date = Timestamp(3_000, 0)),
                tx("mid", 1.0).copy(date = Timestamp(2_000, 0))
            ),
            typeFilter = TransactionTypeFilter.EXPENSE
        )
        assertEquals(listOf("newest", "mid", "old", "undated"), state.filteredTransactions.map { it.id })
    }

    @Test
    fun `payment method filter excludes incomes`() {
        val credit = base.copy(paymentMethodFilter = PaymentMethodFilter.CREDIT).filteredTransactions
        assertEquals(setOf("netflix", "tv"), credit.map { it.id }.toSet())

        val debit = base.copy(paymentMethodFilter = PaymentMethodFilter.DEBIT).filteredTransactions
        assertEquals(setOf("market", "cash"), debit.map { it.id }.toSet())
    }

    @Test
    fun `category filter is OR across selected ids`() {
        val state = base.copy(categoryFilterIds = setOf("food", "stream"))
        assertEquals(setOf("market", "netflix"), state.filteredTransactions.map { it.id }.toSet())
    }

    @Test
    fun `search matches partial titles ignoring case and surrounding spaces`() {
        val state = base.copy(searchQuery = "  NET  ")
        assertEquals(listOf("netflix"), state.filteredTransactions.map { it.id })
        assertTrue(state.hasActiveFilters)
        assertEquals(850.0, state.totalExpense, 0.001)
    }

    @Test
    fun `price search matches partial reais with comma or dot decimals`() {
        val state = base.copy(
            transactions = listOf(
                tx("small", 50.0),
                tx("larger", 150.25),
                tx("thousand", 1050.25),
                tx("other", 25.0),
                tx("50 tickets", 10.0)
            )
        )
        assertEquals(
            listOf("small", "larger", "thousand", "50 tickets"),
            state.copy(searchQuery = "50").filteredTransactions.map { it.id }
        )
        for (query in listOf("50,25", "50.25")) {
            assertEquals(
                listOf("larger", "thousand"),
                state.copy(searchQuery = query).filteredTransactions.map { it.id }
            )
        }
        assertEquals(
            listOf("small"),
            state.copy(searchQuery = "50,00").filteredTransactions.map { it.id }
        )
    }

    @Test
    fun `search combines with filters and updates totals and groups`() {
        val state = base.copy(
            searchQuery = "50",
            paymentMethodFilter = PaymentMethodFilter.CREDIT,
            categoryFilterIds = setOf("stream"),
            groupBy = FilterGroupBy.CATEGORY
        )
        assertEquals(listOf("netflix"), state.filteredTransactions.map { it.id })
        assertEquals(50.0, state.filteredExpense, 0.001)
        assertEquals(-50.0, state.filteredBalance, 0.001)
        assertEquals(listOf(GroupTotal("stream", 50.0, 1)), state.groupedTotals)
        assertTrue(state.copy(searchQuery = "no match").filteredTransactions.isEmpty())
    }

    @Test
    fun `blank search preserves results and does not activate filters`() {
        assertEquals(base.filteredTransactions, base.copy(searchQuery = "  ").filteredTransactions)
        assertFalse(base.copy(searchQuery = "  ").hasActiveFilters)
    }

    @Test
    fun `recurring only includes recurring incomes`() {
        val state = base.copy(recurringOnly = true)
        assertEquals(setOf("salary", "netflix"), state.filteredTransactions.map { it.id }.toSet())
    }

    @Test
    fun `installments only`() {
        val state = base.copy(installmentsOnly = true)
        assertEquals(listOf("tv"), state.filteredTransactions.map { it.id })
    }

    @Test
    fun `combined filters and filtered totals`() {
        val state = base.copy(
            typeFilter = TransactionTypeFilter.EXPENSE,
            cardFilterId = "c1",
            paymentMethodFilter = PaymentMethodFilter.CREDIT
        )
        assertEquals(setOf("netflix", "tv"), state.filteredTransactions.map { it.id }.toSet())
        assertEquals(0.0, state.filteredIncome, 0.001)
        assertEquals(350.0, state.filteredExpense, 0.001)
        assertEquals(-350.0, state.filteredBalance, 0.001)
        assertTrue(state.hasActiveFilters)
        assertFalse(base.hasActiveFilters)
    }

    @Test
    fun `grouped totals by category`() {
        val state = base.copy(typeFilter = TransactionTypeFilter.EXPENSE, groupBy = FilterGroupBy.CATEGORY)
        val groups = state.groupedTotals.associateBy { it.key }
        assertEquals(700.0, groups["home"]!!.total, 0.001)
        assertEquals(2, groups["home"]!!.count)
        assertEquals(100.0, groups[null]!!.total, 0.001)
        assertEquals("home", state.groupedTotals.first().key)
    }

    @Test
    fun `grouped totals by card`() {
        val state = base.copy(groupBy = FilterGroupBy.CARD)
        val groups = state.groupedTotals.associateBy { it.key }
        assertEquals(5350.0, groups["c1"]!!.total, 0.001)
        assertEquals(3, groups["c1"]!!.count)
        assertEquals(400.0, groups["c2"]!!.total, 0.001)
        assertEquals(100.0, groups[null]!!.total, 0.001)
    }

    @Test
    fun `grouped totals empty when groupBy is NONE`() {
        assertTrue(base.groupedTotals.isEmpty())
    }

    @Test
    fun `categories for filter follow type filter`() {
        val state = base.copy(
            availableCategories = listOf(
                CategoryTransactionDto(id = "food", title = "Mercado", transactionType = "EXPENSE"),
                CategoryTransactionDto(id = "sal", title = "Salário", transactionType = "INCOME")
            )
        )
        assertEquals(listOf("food", "sal"), state.categoriesForFilter.map { it.id })
        assertEquals(listOf("sal"), state.copy(typeFilter = TransactionTypeFilter.INCOME).categoriesForFilter.map { it.id })
        assertEquals(listOf("food"), state.copy(typeFilter = TransactionTypeFilter.EXPENSE).categoriesForFilter.map { it.id })
    }
}
