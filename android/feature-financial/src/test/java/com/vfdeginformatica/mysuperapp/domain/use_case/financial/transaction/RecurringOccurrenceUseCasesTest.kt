package com.vfdeginformatica.mysuperapp.domain.use_case.financial.transaction

import com.google.firebase.Timestamp
import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.data.remote.dto.TransactionDto
import com.vfdeginformatica.mysuperapp.domain.model.RecurrenceEditScope
import com.vfdeginformatica.mysuperapp.domain.model.RecurrenceOverride
import com.vfdeginformatica.mysuperapp.domain.model.Transaction
import com.vfdeginformatica.mysuperapp.domain.repository.FakeTransactionRepository
import com.vfdeginformatica.mysuperapp.presentation.screen.new_transaction.contract.TransactionType
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.Date

class RecurringOccurrenceUseCasesTest {

    private val zone: ZoneId = ZoneId.systemDefault()

    private fun timestampOf(year: Int, month: Int, day: Int): Timestamp =
        Timestamp(Date.from(LocalDate.of(year, month, day).atStartOfDay(zone).toInstant()))

    private fun recurring(
        start: YearMonth? = YearMonth.of(2026, 9),
        excluded: Set<YearMonth> = emptySet(),
        overrides: Map<YearMonth, RecurrenceOverride> = emptyMap(),
        isRecurring: Boolean = true,
        occurrenceMonth: YearMonth? = null
    ) = Transaction(
        id = "t1",
        title = "Netflix",
        description = "Streaming",
        type = TransactionType.EXPENSE,
        paymentMethod = "CREDIT",
        amount = 50.0,
        isRecurring = isRecurring,
        date = timestampOf(2026, 9, 15),
        categoryIds = listOf("cat"),
        status = "pending",
        cardId = "c1",
        installments = null,
        installmentNumber = null,
        installmentGroupId = null,
        invoiceMonth = "2026-09",
        recurrenceStart = start,
        recurrenceEnd = null,
        excludedMonths = excluded,
        overrides = overrides,
        occurrenceMonth = occurrenceMonth
    )

    // ---- Delete ----

    @Test
    fun `delete this month adds month to excludedMonths`() = runTest {
        val repo = FakeTransactionRepository()
        val results = DeleteRecurringOccurrenceUseCase(repo)(
            recurring(), YearMonth.of(2026, 11), RecurrenceEditScope.THIS_MONTH
        ).toList()

        assertEquals(listOf("t1" to "2026-11"), repo.excluded)
        assertTrue(repo.deletedIds.isEmpty() && repo.ended.isEmpty())
        assertTrue(results.last() is Resource.Success)
    }

    @Test
    fun `delete this and future on later month ends recurrence at previous month`() = runTest {
        val repo = FakeTransactionRepository()
        DeleteRecurringOccurrenceUseCase(repo)(
            recurring(), YearMonth.of(2026, 11), RecurrenceEditScope.THIS_AND_FUTURE
        ).toList()

        assertEquals(listOf("t1" to "2026-10"), repo.ended)
        assertTrue(repo.deletedIds.isEmpty())
    }

    @Test
    fun `delete this and future on start month deletes the document`() = runTest {
        val repo = FakeTransactionRepository()
        DeleteRecurringOccurrenceUseCase(repo)(
            recurring(), YearMonth.of(2026, 9), RecurrenceEditScope.THIS_AND_FUTURE
        ).toList()

        assertEquals(listOf("t1"), repo.deletedIds)
        assertTrue(repo.ended.isEmpty())
    }

    @Test
    fun `delete rejects non recurring transaction`() = runTest {
        val repo = FakeTransactionRepository()
        val results = DeleteRecurringOccurrenceUseCase(repo)(
            recurring(isRecurring = false), YearMonth.of(2026, 9), RecurrenceEditScope.THIS_MONTH
        ).toList()

        assertTrue(results.last() is Resource.Error)
        assertTrue(repo.excluded.isEmpty() && repo.deletedIds.isEmpty())
    }

    // ---- Update ----

    @Test
    fun `update this month writes an override`() = runTest {
        val repo = FakeTransactionRepository()
        UpdateRecurringOccurrenceUseCase(repo)(
            recurring(), YearMonth.of(2026, 10), "Netflix 4K", 59.9, listOf("cat"), RecurrenceEditScope.THIS_MONTH
        ).toList()

        assertEquals(
            listOf(FakeTransactionRepository.OverrideCall("t1", "2026-10", "Netflix 4K", 59.9)),
            repo.overridden
        )
        // Categorias iguais: template não é tocado
        assertTrue(repo.categoryUpdates.isEmpty())
        assertTrue(repo.splits.isEmpty() && repo.templateUpdates.isEmpty())
    }

    @Test
    fun `update this month with changed categories also updates template categories`() = runTest {
        val repo = FakeTransactionRepository()
        val results = UpdateRecurringOccurrenceUseCase(repo)(
            recurring(), YearMonth.of(2026, 10), "Netflix", 50.0, listOf("stream", "lazer"),
            RecurrenceEditScope.THIS_MONTH
        ).toList()

        assertEquals(listOf("t1" to listOf("stream", "lazer")), repo.categoryUpdates)
        assertEquals(1, repo.overridden.size)
        assertTrue(results.last() is Resource.Success)
    }

    @Test
    fun `update this and future on start month updates template and drops future exceptions`() = runTest {
        val repo = FakeTransactionRepository()
        val t = recurring(
            excluded = setOf(YearMonth.of(2026, 10)),
            overrides = mapOf(YearMonth.of(2026, 12) to RecurrenceOverride(70.0, "X"))
        )
        UpdateRecurringOccurrenceUseCase(repo)(
            t, YearMonth.of(2026, 9), "Netflix", 55.0, listOf("stream"), RecurrenceEditScope.THIS_AND_FUTURE
        ).toList()

        val call = repo.templateUpdates.single()
        assertEquals("t1", call.id)
        assertEquals(55.0, call.amount, 0.0001)
        assertEquals(listOf("stream"), call.category)
        assertTrue(call.excludedMonths.isEmpty())
        assertTrue(call.overrides.isEmpty())
        assertTrue(repo.splits.isEmpty())
    }

    @Test
    fun `update this and future on later month splits into a new recurring document`() = runTest {
        val repo = FakeTransactionRepository()
        val t = recurring(
            excluded = setOf(YearMonth.of(2026, 10), YearMonth.of(2027, 1)),
            overrides = mapOf(YearMonth.of(2026, 10) to RecurrenceOverride(1.0, "old"))
        )
        UpdateRecurringOccurrenceUseCase(repo)(
            t, YearMonth.of(2026, 11), "Netflix Premium", 65.0, listOf("stream"),
            RecurrenceEditScope.THIS_AND_FUTURE
        ).toList()

        val split = repo.splits.single()
        assertEquals("t1", split.originalId)
        assertEquals("2026-10", split.lastMonth)
        // Categorias valem para a série inteira: original também recebe as novas
        assertEquals(listOf("stream"), split.originalCategory)

        val newDto: TransactionDto = split.newTransaction
        assertEquals("Netflix Premium", newDto.title)
        assertEquals(65.0, newDto.amount, 0.0001)
        assertEquals(true, newDto.isRecurring)
        assertEquals("2026-11", newDto.recurrenceStart)
        assertEquals(null, newDto.recurrenceEnd)
        assertEquals(emptyList<String>(), newDto.excludedMonths)
        assertEquals(emptyMap<String, Map<String, Any>>(), newDto.overrides)
        // Campos herdados do original
        assertEquals("CREDIT", newDto.paymentMethod)
        assertEquals("c1", newDto.cardId)
        assertEquals(listOf("stream"), newDto.category)
        assertEquals("EXPENSE", newDto.transactionType)
        assertEquals("Streaming", newDto.description)
        // Data do novo documento começa no mês editado
        val newMonth = YearMonth.from(newDto.transactionDate!!.toDate().toInstant().atZone(zone))
        assertEquals(YearMonth.of(2026, 11), newMonth)
    }

    @Test
    fun `update rejects invalid amount`() = runTest {
        val repo = FakeTransactionRepository()
        val results = UpdateRecurringOccurrenceUseCase(repo)(
            recurring(), YearMonth.of(2026, 10), "Netflix", 0.0, listOf("cat"), RecurrenceEditScope.THIS_MONTH
        ).toList()

        assertTrue(results.last() is Resource.Error)
        assertTrue(repo.overridden.isEmpty())
    }

    @Test
    fun `propagates repository error`() = runTest {
        val repo = FakeTransactionRepository().apply { writeResult = Resource.Error("denied") }
        val results = UpdateRecurringOccurrenceUseCase(repo)(
            recurring(), YearMonth.of(2026, 10), "Netflix", 10.0, listOf("cat"), RecurrenceEditScope.THIS_MONTH
        ).toList()

        val last = results.last()
        assertTrue(last is Resource.Error)
        assertEquals("denied", last.message)
    }
}
