package com.vfdeginformatica.mysuperapp.domain.use_case.financial.transaction

import com.google.firebase.Timestamp
import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.data.remote.dto.TransactionDto
import com.vfdeginformatica.mysuperapp.domain.repository.FakeTransactionRepository
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.Date

class GetTransactionsUseCaseTest {

    private val zone: ZoneId = ZoneId.systemDefault()

    private fun timestampOf(year: Int, month: Int, day: Int): Timestamp =
        Timestamp(Date.from(LocalDate.of(year, month, day).atStartOfDay(zone).toInstant()))

    private fun dto(
        id: String,
        amount: Double,
        date: Timestamp,
        isRecurring: Boolean = false,
        recurrenceStart: String? = null,
        recurrenceEnd: String? = null,
        excludedMonths: List<String>? = null,
        overrides: Map<String, Map<String, Any>>? = null
    ) = TransactionDto(
        id = id,
        title = id,
        transactionType = "EXPENSE",
        paymentMethod = "DEBIT",
        amount = amount,
        isRecurring = isRecurring,
        transactionDate = date,
        recurrenceStart = recurrenceStart,
        recurrenceEnd = recurrenceEnd,
        excludedMonths = excludedMonths,
        overrides = overrides
    )

    @Test
    fun `recurring created in september appears in october`() = runTest {
        val repo = FakeTransactionRepository().apply {
            monthTransactions = emptyList()
            recurringTransactions = listOf(
                dto("rec", 100.0, timestampOf(2026, 9, 10), isRecurring = true, recurrenceStart = "2026-09")
            )
        }

        val result = GetTransactionsUseCase(repo)(YearMonth.of(2026, 10)).toList().last()

        assertTrue(result is Resource.Success)
        val list = result.data.orEmpty()
        assertEquals(listOf("rec"), list.map { it.id })
        assertEquals(YearMonth.of(2026, 10), list.single().occurrenceMonth)
    }

    @Test
    fun `recurring in its origin month is not duplicated and override applies`() = runTest {
        val recurringDto = dto(
            "rec", 100.0, timestampOf(2026, 9, 10), isRecurring = true, recurrenceStart = "2026-09",
            overrides = mapOf("2026-09" to mapOf("amount" to 120.0, "title" to "rec ajustado"))
        )
        val repo = FakeTransactionRepository().apply {
            // A query por data também devolve a recorrente no mês de origem
            monthTransactions = listOf(dto("normal", 10.0, timestampOf(2026, 9, 5)), recurringDto)
            recurringTransactions = listOf(recurringDto)
        }

        val list = GetTransactionsUseCase(repo)(YearMonth.of(2026, 9)).toList().last().data.orEmpty()

        assertEquals(setOf("normal", "rec"), list.map { it.id }.toSet())
        val rec = list.single { it.id == "rec" }
        assertEquals(120.0, rec.amount, 0.0001)
        assertEquals("rec ajustado", rec.title)
    }

    @Test
    fun `excluded and ended recurrences are not shown`() = runTest {
        val repo = FakeTransactionRepository().apply {
            recurringTransactions = listOf(
                dto("excluded", 1.0, timestampOf(2026, 9, 1), isRecurring = true,
                    recurrenceStart = "2026-09", excludedMonths = listOf("2026-10")),
                dto("ended", 1.0, timestampOf(2026, 9, 1), isRecurring = true,
                    recurrenceStart = "2026-09", recurrenceEnd = "2026-09"),
                dto("active", 1.0, timestampOf(2026, 9, 1), isRecurring = true, recurrenceStart = "2026-09")
            )
        }

        val list = GetTransactionsUseCase(repo)(YearMonth.of(2026, 10)).toList().last().data.orEmpty()

        assertEquals(listOf("active"), list.map { it.id })
    }

    @Test
    fun `legacy recurring without recurrenceStart appears from its date month onward`() = runTest {
        val repo = FakeTransactionRepository().apply {
            recurringTransactions = listOf(dto("legacy", 1.0, timestampOf(2026, 9, 1), isRecurring = true))
        }
        val useCase = GetTransactionsUseCase(repo)

        assertTrue(useCase(YearMonth.of(2026, 8)).toList().last().data.orEmpty().isEmpty())
        assertEquals(listOf("legacy"), useCase(YearMonth.of(2026, 12)).toList().last().data.orEmpty().map { it.id })
    }

    @Test
    fun `installments show only in their invoice month`() = runTest {
        fun inst(n: Int, invoice: String) = dto("p$n", 100.0, timestampOf(2026, 9, 10)).copy(
            paymentMethod = "CREDIT", invoiceMonth = invoice,
            installmentGroupId = "g", installments = 3, installmentNumber = n
        )
        val all = listOf(inst(1, "2026-09"), inst(2, "2026-10"), inst(3, "2026-11"))
        val repo = FakeTransactionRepository().apply {
            monthTransactions = all + dto("normal", 10.0, timestampOf(2026, 9, 5))
            invoiceTransactions = all
        }
        val useCase = GetTransactionsUseCase(repo)

        val sept = useCase(YearMonth.of(2026, 9)).toList().last().data.orEmpty()
        assertEquals(setOf("p1", "normal"), sept.map { it.id }.toSet())
        assertEquals(1, sept.count { it.id == "p1" })

        repo.monthTransactions = emptyList()
        val oct = useCase(YearMonth.of(2026, 10)).toList().last().data.orEmpty()
        assertEquals(listOf("p2"), oct.map { it.id })
    }

    @Test
    fun `result is sorted by date descending`() = runTest {
        val repo = FakeTransactionRepository().apply {
            monthTransactions = listOf(
                dto("early", 1.0, timestampOf(2026, 10, 2)),
                dto("late", 1.0, timestampOf(2026, 10, 25))
            )
            recurringTransactions = listOf(
                dto("rec", 1.0, timestampOf(2026, 9, 15), isRecurring = true, recurrenceStart = "2026-09")
            )
        }

        val list = GetTransactionsUseCase(repo)(YearMonth.of(2026, 10)).toList().last().data.orEmpty()

        assertEquals(listOf("late", "rec", "early"), list.map { it.id })
    }
}
