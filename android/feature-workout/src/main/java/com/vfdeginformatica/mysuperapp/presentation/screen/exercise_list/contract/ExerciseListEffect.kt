package com.vfdeginformatica.mysuperapp.presentation.screen.exercise_list.contract

sealed interface ExerciseListEffect {
    data class NavigateToDetail(val exerciseId: String?) : ExerciseListEffect
    data class ShowToast(val message: String) : ExerciseListEffect
}
