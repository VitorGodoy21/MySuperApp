package com.vfdeginformatica.mysuperapp.presentation.screen.body_measurement_list.contract

import com.vfdeginformatica.mysuperapp.domain.model.BodyMeasurement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class BodyMeasurementListUiStateTest {

    private val newest = BodyMeasurement("3", 78.0, 180.0, LocalDate.of(2026, 10, 15))
    private val middle = BodyMeasurement("2", 77.0, 180.0, LocalDate.of(2026, 9, 1))
    private val oldest = BodyMeasurement("1", 79.5, 180.0, LocalDate.of(2026, 8, 1))

    private val state = BodyMeasurementListUiState(measurements = listOf(newest, middle, oldest))

    @Test
    fun `rows compute delta against the previous older record`() {
        val rows = state.rows

        assertEquals(3, rows.size)
        assertEquals(1.0, rows[0].deltaKg!!, 0.0001)
        assertEquals(-2.5, rows[1].deltaKg!!, 0.0001)
        assertNull(rows[2].deltaKg)
    }

    @Test
    fun `chartPoints are chronological`() {
        val points = state.chartPoints

        assertEquals(
            listOf(oldest.measuredAt, middle.measuredAt, newest.measuredAt),
            points.map { it.date }
        )
        assertEquals(listOf(79.5, 77.0, 78.0), points.map { it.weightKg })
    }

    @Test
    fun `latest is the first measurement`() {
        assertEquals(newest, state.latest)
        assertNull(BodyMeasurementListUiState().latest)
    }
}
