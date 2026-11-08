package com.vfdeginformatica.mysuperapp.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class WorkoutWeekTest {

    // 2026-03-02 is a Monday.
    private val monday: LocalDate = LocalDate.of(2026, 3, 2)
    private val week = WorkoutWeekRange(monday)

    private fun session(date: LocalDate, minutes: Int? = 60, calories: Int? = 300) =
        WorkoutSession(id = date.toString(), workoutId = "w", date = date, durationMinutes = minutes, calories = calories)

    @Test
    fun `of aligns any day to the Monday of its week`() {
        assertEquals(DayOfWeek.MONDAY, monday.dayOfWeek)
        assertEquals(monday, WorkoutWeekRange.of(monday).start)
        assertEquals(monday, WorkoutWeekRange.of(monday.plusDays(2)).start)
        assertEquals(monday, WorkoutWeekRange.of(monday.plusDays(6)).start)
        assertEquals(monday.minusWeeks(1), WorkoutWeekRange.of(monday.minusDays(1)).start)
    }

    @Test
    fun `range covers Monday through Sunday and indexes days Monday first`() {
        assertEquals(monday.plusDays(6), week.end)
        assertTrue(week.contains(monday))
        assertTrue(week.contains(monday.plusDays(6)))
        assertFalse(week.contains(monday.minusDays(1)))
        assertFalse(week.contains(monday.plusDays(7)))
        assertEquals(0, week.indexOf(monday))
        assertEquals(6, week.indexOf(monday.plusDays(6)))
        assertNull(week.indexOf(monday.plusDays(7)))
        assertEquals(monday.minusWeeks(1), week.previous().start)
        assertEquals(monday.plusWeeks(1), week.next().start)
    }

    @Test
    fun `build sums only sessions inside the week treating null stats as zero`() {
        val summary = WorkoutWeekSummary.build(
            week,
            listOf(
                session(monday, minutes = 40, calories = 200),
                session(monday, minutes = null, calories = null),
                session(monday.plusDays(3), minutes = 50, calories = 100),
                session(monday.plusDays(7)),
                session(monday.minusDays(1)),
            ),
        )

        assertEquals(3, summary.sessions)
        assertEquals(90, summary.minutes)
        assertEquals(300, summary.calories)
        assertEquals(listOf(40, 0, 0, 50, 0, 0, 0), summary.minutesByDay)
        assertEquals(listOf(2, 0, 0, 1, 0, 0, 0), summary.sessionsByDay)
        assertTrue(summary.hasSessions)
    }

    @Test
    fun `compareTo returns deltas against the previous week or null without one`() {
        val current = WorkoutWeekSummary.build(week, listOf(session(monday, 60, 300), session(monday.plusDays(1), 30, 100)))
        val previous = WorkoutWeekSummary.build(week.previous(), listOf(session(monday.minusDays(3), 90, 500)))

        assertNull(current.compareTo(null))
        assertEquals(WorkoutWeekComparison(sessionsDelta = 1, minutesDelta = 0, caloriesDelta = -100), current.compareTo(previous))
    }
}
