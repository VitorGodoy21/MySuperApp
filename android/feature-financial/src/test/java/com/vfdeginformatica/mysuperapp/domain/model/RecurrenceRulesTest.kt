package com.vfdeginformatica.mysuperapp.domain.model

import com.google.firebase.Timestamp
import com.vfdeginformatica.mysuperapp.presentation.screen.new_transaction.contract.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.Date

class RecurrenceRulesTest {

    private val zone: ZoneId = ZoneId.systemDefault()

    private fun timestampOf(year: Int, month: Int, day: Int): Timestamp =
        Timestamp(Date.from(LocalDate.of(year, month, day).atStartOfDay(zone).toInstant()))

    private fun recurring(
        date: Timestamp? = timestampOf(2026, 9, 15),
        start: YearMonth? = YearMonth.of(2026, 9),
        end: YearMonth? = null,
        excluded: Set<YearMonth> = emptySet(),
        overrides: Map<YearMonth, RecurrenceOverride> = emptyMap(),
        isRecurring: Boolean = true
    ) = Transaction(
        id = "t1",
        title = "Netflix",
        description = "",
        type = TransactionType.EXPENSE,
        paymentMethod = "CREDIT",
        amount = 50.0,
        isRecurring = isRecurring,
        date = date,
        categoryIds = emptyList(),
        status = "pending",
        cardId = "c1",
        installments = null,
        installmentNumber = null,
        installmentGroupId = null,
        invoiceMonth = "2026-09",
        recurrenceStart = start,
        recurrenceEnd = end,
        excludedMonths = excluded,
        overrides = overrides
    )

    @Test
    fun `occurs from start month onward without end`() {
        val t = recurring()
        assertFalse(t.occursIn(YearMonth.of(2026, 8)))
        assertTrue(t.occursIn(YearMonth.of(2026, 9)))
        assertTrue(t.occursIn(YearMonth.of(2026, 10)))
        assertTrue(t.occursIn(YearMonth.of(2030, 1)))
    }

    @Test
    fun `respects recurrence end inclusive`() {
        val t = recurring(end = YearMonth.of(2026, 11))
        assertTrue(t.occursIn(YearMonth.of(2026, 11)))
        assertFalse(t.occursIn(YearMonth.of(2026, 12)))
    }

    @Test
    fun `skips excluded months only`() {
        val t = recurring(excluded = setOf(YearMonth.of(2026, 10)))
        assertTrue(t.occursIn(YearMonth.of(2026, 9)))
        assertFalse(t.occursIn(YearMonth.of(2026, 10)))
        assertTrue(t.occursIn(YearMonth.of(2026, 11)))
    }

    @Test
    fun `legacy document without recurrenceStart uses transactionDate month`() {
        val t = recurring(start = null, date = timestampOf(2026, 9, 20))
        assertEquals(YearMonth.of(2026, 9), t.effectiveRecurrenceStart)
        assertFalse(t.occursIn(YearMonth.of(2026, 8)))
        assertTrue(t.occursIn(YearMonth.of(2026, 10)))
    }

    @Test
    fun `non recurring never occurs`() {
        val t = recurring(isRecurring = false)
        assertFalse(t.occursIn(YearMonth.of(2026, 9)))
    }

    @Test
    fun `materialize applies override and marks occurrence month`() {
        val t = recurring(
            overrides = mapOf(YearMonth.of(2026, 10) to RecurrenceOverride(amount = 59.9, title = "Netflix 4K"))
        )
        val oct = t.materializeFor(YearMonth.of(2026, 10), zone)
        assertEquals(59.9, oct.amount, 0.0001)
        assertEquals("Netflix 4K", oct.title)
        assertEquals(YearMonth.of(2026, 10), oct.occurrenceMonth)
        assertTrue(oct.hasOverrideForOccurrence)

        val nov = t.materializeFor(YearMonth.of(2026, 11), zone)
        assertEquals(50.0, nov.amount, 0.0001)
        assertEquals("Netflix", nov.title)
        assertFalse(nov.hasOverrideForOccurrence)
    }

    @Test
    fun `materialize keeps original day clamped to month length`() {
        val t = recurring(date = timestampOf(2026, 1, 31))
        val feb = t.materializeFor(YearMonth.of(2026, 2), zone)
        val localDate = feb.date!!.toDate().toInstant().atZone(zone).toLocalDate()
        assertEquals(LocalDate.of(2026, 2, 28), localDate)
    }

    @Test
    fun `parse and format month round trip`() {
        assertEquals("2026-10", RecurrenceRules.formatMonth(YearMonth.of(2026, 10)))
        assertEquals(YearMonth.of(2026, 10), RecurrenceRules.parseMonth("2026-10"))
        assertNull(RecurrenceRules.parseMonth("invalid"))
        assertNull(RecurrenceRules.parseMonth(null))
    }
}
