package com.vfdeginformatica.mysuperapp.presentation.screen.exercise_detail.contract

import com.vfdeginformatica.mysuperapp.domain.model.MuscleGroup

data class ExerciseDetailUiState(
    val exerciseId: String? = null,
    val isLoading: Boolean = false,
    val name: String = "",
    val muscleGroup: MuscleGroup? = null,
    val description: String = "",
    val notes: String = "",
    val nameError: String? = null,
    val duplicateWarning: String? = null,
    val usedInWorkouts: List<String> = emptyList(),
    val isSaving: Boolean = false,
    val isDeleting: Boolean = false,
    val showDeleteConfirmation: Boolean = false,
    val isSubmitEnabled: Boolean = false,
) {
    val isEditing: Boolean
        get() = !exerciseId.isNullOrBlank()

    val isBusy: Boolean
        get() = isSaving || isDeleting

    val canDelete: Boolean
        get() = isEditing && usedInWorkouts.isEmpty()
}
