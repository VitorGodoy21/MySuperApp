package com.vfdeginformatica.mysuperapp.presentation.common

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class FinancialDatesTest {

    private val zone: ZoneId = ZoneId.of("America/Sao_Paulo")
    private val now: Instant = LocalDate.of(2026, 3, 5).atTime(14, 37, 12).atZone(zone).toInstant()
    private val clock: Clock = Clock.fixed(now, zone)

    @Test
    fun `today keeps the current time of day`() {
        val result = LocalDate.of(2026, 3, 5).toTransactionTimestamp(clock)

        assertEquals(now, result.toDate().toInstant())
    }

    @Test
    fun `past date is stored at local start of day`() {
        val date = LocalDate.of(2026, 2, 28)

        val result = date.toTransactionTimestamp(clock)

        assertEquals(date.atStartOfDay(zone).toInstant(), result.toDate().toInstant())
    }

    @Test
    fun `future date is stored at local start of day`() {
        val date = LocalDate.of(2026, 4, 10)

        val result = date.toTransactionTimestamp(clock)

        assertEquals(date.atStartOfDay(zone).toInstant(), result.toDate().toInstant())
        assertEquals(date, result.toDate().toInstant().atZone(zone).toLocalDate())
    }
}
