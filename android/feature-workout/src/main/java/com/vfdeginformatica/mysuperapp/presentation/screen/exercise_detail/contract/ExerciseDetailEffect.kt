package com.vfdeginformatica.mysuperapp.presentation.screen.exercise_detail.contract

sealed interface ExerciseDetailEffect {
    data class ShowToast(val message: String) : ExerciseDetailEffect
    data object NavigateUp : ExerciseDetailEffect
}
