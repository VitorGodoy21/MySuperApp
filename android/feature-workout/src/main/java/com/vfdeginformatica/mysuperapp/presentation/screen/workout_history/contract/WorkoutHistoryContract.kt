package com.vfdeginformatica.mysuperapp.presentation.screen.workout_history.contract

import com.vfdeginformatica.mysuperapp.domain.model.Workout
import com.vfdeginformatica.mysuperapp.domain.model.WorkoutIcon
import com.vfdeginformatica.mysuperapp.domain.model.WorkoutSession
import com.vfdeginformatica.mysuperapp.domain.model.WorkoutWeekComparison
import com.vfdeginformatica.mysuperapp.domain.model.WorkoutWeekRange
import com.vfdeginformatica.mysuperapp.domain.model.WorkoutWeekSummary
import java.time.LocalDate

enum class HistoryMode { DAYS, WEEKS }

/** Recent gym sessions of every workout, grouped by day (reached from the hub's "Histórico" card). */
data class WorkoutHistoryUiState(
    val isLoading: Boolean = false,
    val workouts: List<Workout> = emptyList(),
    val isSessionsLoading: Boolean = false,
    /** Recent sessions of every workout, already ordered newest first by the use case. */
    val sessions: List<WorkoutSession> = emptyList(),
    val isDeletingSession: Boolean = false,
    val sessionPendingDelete: WorkoutSession? = null,
    val mode: HistoryMode = HistoryMode.DAYS,
    val today: LocalDate = LocalDate.now(),
    /** Monday of the week shown in [HistoryMode.WEEKS]. */
    val selectedWeekStart: LocalDate = WorkoutWeekRange.of(today).start,
    val isWeekSessionsLoading: Boolean = false,
    /** Null until the weekly window has been loaded once. */
    val weekSessions: List<WorkoutSession>? = null,
) {
    val isBusy: Boolean get() = isDeletingSession

    /** Sessions grouped by day, preserving the incoming (newest first) order. */
    val historyDays: List<WorkoutHistoryDay> get() = groupByDay(sessions)

    val selectedWeek: WorkoutWeekRange get() = WorkoutWeekRange(selectedWeekStart)

    val currentWeekStart: LocalDate get() = WorkoutWeekRange.of(today).start

    /** Monday of the oldest selectable week. */
    val oldestWeekStart: LocalDate get() = currentWeekStart.minusWeeks((WorkoutWeekRange.WEEKS_BACK - 1).toLong())

    /** First day that must be loaded so the oldest week still has a previous-week comparison. */
    val weekWindowStart: LocalDate get() = oldestWeekStart.minusWeeks(1)

    val canGoPreviousWeek: Boolean get() = selectedWeekStart.isAfter(oldestWeekStart)

    val canGoNextWeek: Boolean get() = selectedWeekStart.isBefore(currentWeekStart)

    val weekSummary: WorkoutWeekSummary
        get() = WorkoutWeekSummary.build(selectedWeek, weekSessions.orEmpty())

    val previousWeekSummary: WorkoutWeekSummary?
        get() {
            val previous = selectedWeek.previous()
            if (previous.start.isBefore(weekWindowStart)) return null
            return WorkoutWeekSummary.build(previous, weekSessions.orEmpty())
        }

    val weekComparison: WorkoutWeekComparison? get() = weekSummary.compareTo(previousWeekSummary)

    /** Selected week's sessions grouped by day, newest first. */
    val weekHistoryDays: List<WorkoutHistoryDay>
        get() = groupByDay(weekSessions.orEmpty().filter { selectedWeek.contains(it.date) })

    fun findSession(id: String): WorkoutSession? =
        sessions.find { it.id == id } ?: weekSessions?.find { it.id == id }

    private fun groupByDay(source: List<WorkoutSession>): List<WorkoutHistoryDay> {
        val names = workouts.associate { it.id to it.name }
        val icons = workouts.associate { it.id to it.icon }
        return source
            .groupBy { it.date }
            .map { (date, daySessions) ->
                WorkoutHistoryDay(
                    date = date,
                    entries = daySessions.map { session ->
                        WorkoutHistoryEntry(
                            session = session,
                            workoutName = names[session.workoutId],
                            workoutIcon = icons[session.workoutId] ?: WorkoutIcon.DEFAULT,
                        )
                    },
                )
            }
    }
}

data class WorkoutHistoryDay(
    val date: LocalDate,
    val entries: List<WorkoutHistoryEntry>,
)

data class WorkoutHistoryEntry(
    val session: WorkoutSession,
    /** Null when the workout no longer exists. */
    val workoutName: String?,
    val workoutIcon: WorkoutIcon = WorkoutIcon.DEFAULT,
) {
    val workoutExists: Boolean get() = workoutName != null
}

sealed interface WorkoutHistoryEvent {
    data object Reload : WorkoutHistoryEvent
    data class SessionClicked(val sessionId: String) : WorkoutHistoryEvent
    data class DeleteSessionClicked(val sessionId: String) : WorkoutHistoryEvent
    data object DeleteSessionDismissed : WorkoutHistoryEvent
    data object DeleteSessionConfirmed : WorkoutHistoryEvent
    data class ModeChanged(val mode: HistoryMode) : WorkoutHistoryEvent
    data object PreviousWeek : WorkoutHistoryEvent
    data object NextWeek : WorkoutHistoryEvent
}

sealed interface WorkoutHistoryEffect {
    data class NavigateToWorkout(val workoutId: String) : WorkoutHistoryEffect
    data class ShowToast(val message: String) : WorkoutHistoryEffect
}
