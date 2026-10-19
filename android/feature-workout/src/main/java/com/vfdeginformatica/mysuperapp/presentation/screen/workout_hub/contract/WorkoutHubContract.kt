package com.vfdeginformatica.mysuperapp.presentation.screen.workout_hub.contract

import com.vfdeginformatica.mysuperapp.domain.model.BodyMeasurement
import com.vfdeginformatica.mysuperapp.domain.model.Workout

data class WorkoutHubUiState(
    val isLoading: Boolean = false,
    val latestMeasurement: BodyMeasurement? = null,
    val exerciseCount: Int = 0,
    val workouts: List<Workout> = emptyList(),
) {
    val workoutCount: Int get() = workouts.size
}

sealed interface WorkoutHubEvent {
    data object Reload : WorkoutHubEvent
    data object BodyMeasurementsClicked : WorkoutHubEvent
    data object ExercisesClicked : WorkoutHubEvent
    data object WorkoutsClicked : WorkoutHubEvent
    data class WorkoutClicked(val workoutId: String) : WorkoutHubEvent
}

sealed interface WorkoutHubEffect {
    data object NavigateToBodyMeasurements : WorkoutHubEffect
    data object NavigateToExercises : WorkoutHubEffect
    data object NavigateToWorkouts : WorkoutHubEffect
    data class NavigateToWorkout(val workoutId: String) : WorkoutHubEffect
    data class ShowToast(val message: String) : WorkoutHubEffect
}
