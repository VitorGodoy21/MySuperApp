package com.vfdeginformatica.mysuperapp.presentation.screen.workout_list.contract

sealed interface WorkoutListEffect {
    /** `null` id means "create a new workout". */
    data class NavigateToDetail(val workoutId: String?) : WorkoutListEffect
    data class ShowToast(val message: String) : WorkoutListEffect
}
