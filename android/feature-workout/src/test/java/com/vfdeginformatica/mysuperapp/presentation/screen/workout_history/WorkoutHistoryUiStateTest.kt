package com.vfdeginformatica.mysuperapp.presentation.screen.workout_history

import com.vfdeginformatica.mysuperapp.domain.model.Workout
import com.vfdeginformatica.mysuperapp.domain.model.WorkoutSession
import com.vfdeginformatica.mysuperapp.domain.model.WorkoutWeekRange
import com.vfdeginformatica.mysuperapp.presentation.screen.workout_history.contract.WorkoutHistoryUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class WorkoutHistoryUiStateTest {

    // Wednesday 2026-03-04 → current week starts Monday 2026-03-02.
    private val today: LocalDate = LocalDate.of(2026, 3, 4)
    private val currentMonday: LocalDate = LocalDate.of(2026, 3, 2)

    private fun session(id: String, date: LocalDate, minutes: Int = 30) =
        WorkoutSession(id = id, workoutId = "wA", date = date, durationMinutes = minutes)

    private val base = WorkoutHistoryUiState(
        today = today,
        workouts = listOf(Workout(id = "wA", name = "Treino A")),
    )

    @Test
    fun `defaults to the current week and limits navigation to the configured range`() {
        assertEquals(currentMonday, base.selectedWeekStart)
        assertFalse(base.canGoNextWeek)
        assertTrue(base.canGoPreviousWeek)

        val oldest = base.copy(selectedWeekStart = base.oldestWeekStart)
        assertEquals(currentMonday.minusWeeks((WorkoutWeekRange.WEEKS_BACK - 1).toLong()), oldest.selectedWeekStart)
        assertFalse(oldest.canGoPreviousWeek)
        assertTrue(oldest.canGoNextWeek)
        assertEquals(oldest.selectedWeekStart.minusWeeks(1), base.weekWindowStart)
    }

    @Test
    fun `week views only use sessions of the selected week and keep incoming order`() {
        val state = base.copy(
            weekSessions = listOf(
                session("s3", currentMonday.plusDays(2), minutes = 20),
                session("s2", currentMonday, minutes = 40),
                session("s1", currentMonday.minusDays(1), minutes = 90),
                session("s0", currentMonday.minusDays(6), minutes = 10),
            ),
        )

        assertEquals(listOf(currentMonday.plusDays(2), currentMonday), state.weekHistoryDays.map { it.date })
        assertEquals(listOf("s3", "s2"), state.weekHistoryDays.flatMap { d -> d.entries.map { it.session.id } })
        assertEquals("Treino A", state.weekHistoryDays.first().entries.single().workoutName)
        assertEquals(2, state.weekSummary.sessions)
        assertEquals(60, state.weekSummary.minutes)

        val comparison = state.weekComparison
        assertNotNull(comparison)
        assertEquals(0, comparison!!.sessionsDelta)
        assertEquals(-40, comparison.minutesDelta)

        assertTrue(state.historyDays.isEmpty())
        assertEquals("s1", state.findSession("s1")?.id)
    }

    @Test
    fun `previous week comparison is unavailable before the loaded window`() {
        val oldest = base.copy(selectedWeekStart = base.oldestWeekStart, weekSessions = emptyList())
        assertNotNull(oldest.previousWeekSummary)

        val beyond = base.copy(selectedWeekStart = base.oldestWeekStart.minusWeeks(1), weekSessions = emptyList())
        assertNull(beyond.previousWeekSummary)
        assertNull(beyond.weekComparison)
    }
}
