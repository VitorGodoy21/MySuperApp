package com.vfdeginformatica.mysuperapp.data.mapper

import com.vfdeginformatica.mysuperapp.data.remote.dto.WorkoutSessionDto
import com.vfdeginformatica.mysuperapp.domain.model.WorkoutSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class WorkoutSessionMapperTest {

    @Test
    fun `round trip keeps date and optional stats`() {
        val session = WorkoutSession(
            id = "s1",
            workoutId = "w1",
            date = LocalDate.of(2026, 10, 18),
            durationMinutes = 75,
            calories = 420,
        )

        assertEquals(session, session.toDto().toModel())
    }

    @Test
    fun `non-positive stats and missing date fall back`() {
        val dto = WorkoutSessionDto(
            id = "s2",
            workoutId = "w1",
            date = null,
            createdAt = LocalDate.of(2026, 1, 2).toTimestamp(),
            durationMinutes = 0,
            calories = -5,
        )

        val model = dto.toModel()

        assertEquals(LocalDate.of(2026, 1, 2), model.date)
        assertNull(model.durationMinutes)
        assertNull(model.calories)
    }
}
