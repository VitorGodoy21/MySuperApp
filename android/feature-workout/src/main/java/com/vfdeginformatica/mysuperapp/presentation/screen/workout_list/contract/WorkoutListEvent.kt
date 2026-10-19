package com.vfdeginformatica.mysuperapp.presentation.screen.workout_list.contract

sealed interface WorkoutListEvent {
    data object Reload : WorkoutListEvent
    data class WorkoutClicked(val workoutId: String) : WorkoutListEvent
    data object NewClicked : WorkoutListEvent
}
