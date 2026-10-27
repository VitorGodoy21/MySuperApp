package com.vfdeginformatica.mysuperapp.presentation.screen.workout_history.contract

import com.vfdeginformatica.mysuperapp.domain.model.Workout
import com.vfdeginformatica.mysuperapp.domain.model.WorkoutIcon
import com.vfdeginformatica.mysuperapp.domain.model.WorkoutSession
import java.time.LocalDate

/** Recent gym sessions of every workout, grouped by day (reached from the hub's "Histórico" card). */
data class WorkoutHistoryUiState(
    val isLoading: Boolean = false,
    val workouts: List<Workout> = emptyList(),
    val isSessionsLoading: Boolean = false,
    /** Recent sessions of every workout, already ordered newest first by the use case. */
    val sessions: List<WorkoutSession> = emptyList(),
    val isDeletingSession: Boolean = false,
    val sessionPendingDelete: WorkoutSession? = null,
) {
    val isBusy: Boolean get() = isDeletingSession

    /** Sessions grouped by day, preserving the incoming (newest first) order. */
    val historyDays: List<WorkoutHistoryDay>
        get() {
            val names = workouts.associate { it.id to it.name }
            val icons = workouts.associate { it.id to it.icon }
            return sessions
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
}

sealed interface WorkoutHistoryEffect {
    data class NavigateToWorkout(val workoutId: String) : WorkoutHistoryEffect
    data class ShowToast(val message: String) : WorkoutHistoryEffect
}
