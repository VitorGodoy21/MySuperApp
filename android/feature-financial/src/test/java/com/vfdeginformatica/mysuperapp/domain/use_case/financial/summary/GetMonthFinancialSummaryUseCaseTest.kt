package com.vfdeginformatica.mysuperapp.domain.use_case.financial.summary

import com.google.firebase.Timestamp
import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.data.remote.dto.TransactionDto
import com.vfdeginformatica.mysuperapp.domain.repository.FakeTransactionRepository
import com.vfdeginformatica.mysuperapp.domain.use_case.financial.transaction.GetInvoiceProjectionUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.financial.transaction.GetTransactionsUseCase
import com.vfdeginformatica.mysuperapp.presentation.screen.financial.contract.FinancialUiState
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.Date

class GetMonthFinancialSummaryUseCaseTest {

    private val month = YearMonth.of(2026, 10)

    private fun timestampOf(day: Int): Timestamp =
        Timestamp(Date.from(LocalDate.of(2026, 10, day).atStartOfDay(ZoneId.systemDefault()).toInstant()))

    private fun dto(
        id: String,
        amount: Double,
        type: String = "EXPENSE",
        paymentMethod: String = "DEBIT",
        invoiceMonth: String? = null,
        isRecurring: Boolean = false,
        recurrenceStart: String? = null,
    ) = TransactionDto(
        id = id,
        title = id,
        transactionType = type,
        paymentMethod = paymentMethod,
        amount = amount,
        isRecurring = isRecurring,
        transactionDate = timestampOf(5),
        invoiceMonth = invoiceMonth,
        recurrenceStart = recurrenceStart,
    )

    private fun useCase(repo: FakeTransactionRepository) =
        GetMonthFinancialSummaryUseCase(GetTransactionsUseCase(repo), GetInvoiceProjectionUseCase(repo))

    @Test
    fun `balance matches dashboard cash rule and ignores credit purchases of the month`() = runTest {
        val repo = FakeTransactionRepository().apply {
            monthTransactions = listOf(
                dto("salary", 5000.0, type = "INCOME"),
                dto("market", 300.0),
                dto("tv", 2000.0, paymentMethod = "CREDIT", invoiceMonth = "2026-11"),
            )
            // Invoice closed in September (paid in October) + a recurring credit expense
            invoiceTransactions = listOf(dto("sept-purchase", 400.0, paymentMethod = "CREDIT", invoiceMonth = "2026-09"))
            recurringTransactions = listOf(
                dto("stream", 50.0, paymentMethod = "CREDIT", isRecurring = true, recurrenceStart = "2026-01")
            )
        }

        val result = useCase(repo)(month).toList().last()

        assertTrue(result is Resource.Success)
        val summary = result.data!!
        assertEquals(5000.0, summary.totalIncome, 0.001)
        assertEquals(300.0 + 400.0 + 50.0, summary.cashExpense, 0.001)
        assertEquals(5000.0 - 750.0, summary.balance, 0.001)

        // Same numbers the dashboard would show for that month
        val transactions = GetTransactionsUseCase(repo)(month).toList().last().data.orEmpty()
        val paid = GetInvoiceProjectionUseCase(repo)(month.minusMonths(1)).toList().last().data!!
        val dashboard = FinancialUiState(
            selectedMonth = month,
            transactions = transactions,
            paidInvoiceTransactions = paid.invoiceTransactions,
            paidInvoiceRecurringTransactions = paid.recurringTransactions,
        )
        assertEquals(dashboard.balance, summary.balance, 0.001)
        assertEquals(dashboard.cashExpense, summary.cashExpense, 0.001)
    }

    @Test
    fun `transactions error is surfaced`() = runTest {
        val repo = object : FakeTransactionRepository() {
            override suspend fun getTransactions(start: Timestamp, end: Timestamp): Resource<List<TransactionDto>> =
                Resource.Error("boom")
        }

        val result = useCase(repo)(month).toList().last()

        assertTrue(result is Resource.Error)
        assertEquals("boom", result.message)
    }
}
