package com.vfdeginformatica.mysuperapp.presentation.screen.exercise_detail.contract

import com.vfdeginformatica.mysuperapp.domain.model.MuscleGroup

sealed interface ExerciseDetailEvent {
    data class NameChanged(val name: String) : ExerciseDetailEvent
    data class MuscleGroupChanged(val muscleGroup: MuscleGroup?) : ExerciseDetailEvent
    data class DescriptionChanged(val description: String) : ExerciseDetailEvent
    data class NotesChanged(val notes: String) : ExerciseDetailEvent
    data object SaveClicked : ExerciseDetailEvent
    data object DeleteClicked : ExerciseDetailEvent
    data object DeleteConfirmed : ExerciseDetailEvent
    data object DeleteDismissed : ExerciseDetailEvent
}
