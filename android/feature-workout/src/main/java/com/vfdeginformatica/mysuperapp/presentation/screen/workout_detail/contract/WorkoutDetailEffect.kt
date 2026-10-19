package com.vfdeginformatica.mysuperapp.presentation.screen.workout_detail.contract

sealed interface WorkoutDetailEffect {
    data class ShowToast(val message: String) : WorkoutDetailEffect
    data object NavigateUp : WorkoutDetailEffect
}
