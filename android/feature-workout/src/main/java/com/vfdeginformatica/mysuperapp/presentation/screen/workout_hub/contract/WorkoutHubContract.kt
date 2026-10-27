package com.vfdeginformatica.mysuperapp.presentation.screen.workout_hub.contract

import com.vfdeginformatica.mysuperapp.domain.model.Workout

/** "Academia" entry point: the user's workouts in view mode (editing lives in the drawer → Treinos). */
data class WorkoutHubUiState(
    val isLoading: Boolean = false,
    val workouts: List<Workout> = emptyList(),
    /** UI-only expansion state of the "Outros" group; starts collapsed on every visit. */
    val isOthersExpanded: Boolean = false,
) {
    val mainWorkouts: List<Workout> get() = workouts.filterNot { it.isOther }
    val otherWorkouts: List<Workout> get() = workouts.filter { it.isOther }
}

sealed interface WorkoutHubEvent {
    data object Reload : WorkoutHubEvent
    data object OthersToggled : WorkoutHubEvent
    data object DietClicked : WorkoutHubEvent
    data object BodyMeasurementsClicked : WorkoutHubEvent
    data object HistoryClicked : WorkoutHubEvent
    data class WorkoutClicked(val workoutId: String) : WorkoutHubEvent
}

sealed interface WorkoutHubEffect {
    data object NavigateToDiet : WorkoutHubEffect
    data object NavigateToBodyMeasurements : WorkoutHubEffect
    data object NavigateToHistory : WorkoutHubEffect
    data class NavigateToWorkout(val workoutId: String) : WorkoutHubEffect
    data class ShowToast(val message: String) : WorkoutHubEffect
}
