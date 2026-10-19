package com.vfdeginformatica.mysuperapp.presentation.screen.exercise_list.contract

sealed interface ExerciseListEvent {
    data object Reload : ExerciseListEvent
    data class QueryChanged(val query: String) : ExerciseListEvent
    data class ExerciseClicked(val exerciseId: String) : ExerciseListEvent
    data object NewClicked : ExerciseListEvent
}
