package com.vfdeginformatica.mysuperapp.presentation.screen.workout_hub.contract

import com.vfdeginformatica.mysuperapp.domain.model.Workout
import com.vfdeginformatica.mysuperapp.domain.model.WorkoutSession
import java.time.LocalDate

/** "Academia" entry point: the user's workouts in view mode (editing lives in the drawer → Treinos). */
data class WorkoutHubUiState(
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
            return sessions
                .groupBy { it.date }
                .map { (date, daySessions) ->
                    WorkoutHistoryDay(
                        date = date,
                        entries = daySessions.map { session ->
                            WorkoutHistoryEntry(session = session, workoutName = names[session.workoutId])
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
) {
    val workoutExists: Boolean get() = workoutName != null
}

sealed interface WorkoutHubEvent {
    data object Reload : WorkoutHubEvent
    data class WorkoutClicked(val workoutId: String) : WorkoutHubEvent
    data class SessionClicked(val sessionId: String) : WorkoutHubEvent
    data class DeleteSessionClicked(val sessionId: String) : WorkoutHubEvent
    data object DeleteSessionDismissed : WorkoutHubEvent
    data object DeleteSessionConfirmed : WorkoutHubEvent
}

sealed interface WorkoutHubEffect {
    data class NavigateToWorkout(val workoutId: String) : WorkoutHubEffect
    data class ShowToast(val message: String) : WorkoutHubEffect
}
