package com.vfdeginformatica.mysuperapp.presentation.screen.workout_hub.contract

import com.vfdeginformatica.mysuperapp.domain.model.Workout

/** "Academia" entry point: the user's workouts in view mode (editing lives in the drawer → Treinos). */
data class WorkoutHubUiState(
    val isLoading: Boolean = false,
    val workouts: List<Workout> = emptyList(),
)

sealed interface WorkoutHubEvent {
    data object Reload : WorkoutHubEvent
    data class WorkoutClicked(val workoutId: String) : WorkoutHubEvent
}

sealed interface WorkoutHubEffect {
    data class NavigateToWorkout(val workoutId: String) : WorkoutHubEffect
    data class ShowToast(val message: String) : WorkoutHubEffect
}
