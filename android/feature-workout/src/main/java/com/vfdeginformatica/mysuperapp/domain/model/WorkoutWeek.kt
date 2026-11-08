package com.vfdeginformatica.mysuperapp.domain.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/** Monday→Sunday calendar week used by the weekly view of the workout history. */
data class WorkoutWeekRange(val start: LocalDate) {
    init {
        require(start.dayOfWeek == DayOfWeek.MONDAY) { "Week must start on Monday" }
    }

    companion object {
        /** How many weeks back (current one included) the weekly history can navigate. */
        const val WEEKS_BACK = 12

        fun of(date: LocalDate): WorkoutWeekRange =
            WorkoutWeekRange(date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)))
    }

    val end: LocalDate get() = start.plusDays(6)

    fun contains(date: LocalDate): Boolean = !date.isBefore(start) && !date.isAfter(end)

    fun isCurrent(today: LocalDate): Boolean = contains(today)

    fun previous(): WorkoutWeekRange = WorkoutWeekRange(start.minusWeeks(1))

    fun next(): WorkoutWeekRange = WorkoutWeekRange(start.plusWeeks(1))

    /** 0 = Monday … 6 = Sunday; null when [date] is outside this week. */
    fun indexOf(date: LocalDate): Int? = if (contains(date)) date.dayOfWeek.value - 1 else null
}

/** Totals of one calendar week; null minutes/calories count as 0, like the Home summary. */
data class WorkoutWeekSummary(
    val range: WorkoutWeekRange,
    val sessions: Int,
    val minutes: Int,
    val calories: Int,
    /** Monday-first, 7 entries. */
    val minutesByDay: List<Int>,
    /** Monday-first, 7 entries. */
    val sessionsByDay: List<Int>,
) {
    val hasSessions: Boolean get() = sessions > 0

    fun compareTo(previous: WorkoutWeekSummary?): WorkoutWeekComparison? =
        previous?.let {
            WorkoutWeekComparison(
                sessionsDelta = sessions - it.sessions,
                minutesDelta = minutes - it.minutes,
                caloriesDelta = calories - it.calories,
            )
        }

    companion object {
        fun build(range: WorkoutWeekRange, sessions: List<WorkoutSession>): WorkoutWeekSummary {
            val minutesByDay = MutableList(7) { 0 }
            val sessionsByDay = MutableList(7) { 0 }
            var total = 0
            var minutes = 0
            var calories = 0
            sessions.forEach { session ->
                val index = range.indexOf(session.date) ?: return@forEach
                total++
                minutes += session.durationMinutes ?: 0
                calories += session.calories ?: 0
                sessionsByDay[index]++
                minutesByDay[index] += session.durationMinutes ?: 0
            }
            return WorkoutWeekSummary(
                range = range,
                sessions = total,
                minutes = minutes,
                calories = calories,
                minutesByDay = minutesByDay,
                sessionsByDay = sessionsByDay,
            )
        }
    }
}

/** Selected week minus previous week; positive means the selected week was higher. */
data class WorkoutWeekComparison(
    val sessionsDelta: Int,
    val minutesDelta: Int,
    val caloriesDelta: Int,
)
