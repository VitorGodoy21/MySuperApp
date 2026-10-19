package com.vfdeginformatica.mysuperapp.presentation.screen.exercise_detail.contract

import com.vfdeginformatica.mysuperapp.domain.model.MuscleGroup
import com.vfdeginformatica.mysuperapp.domain.model.YouTubeVideo

data class ExerciseDetailUiState(
    val exerciseId: String? = null,
    val isLoading: Boolean = false,
    val name: String = "",
    val muscleGroup: MuscleGroup? = null,
    val description: String = "",
    val notes: String = "",
    val videoUrl: String = "",
    val nameError: String? = null,
    val videoUrlError: String? = null,
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

    /** Parsed YouTube id of [videoUrl]; null when blank or not a YouTube link. */
    val videoId: String?
        get() = YouTubeVideo.parseVideoId(videoUrl)

    /** Blank is allowed (no video); otherwise it must resolve to a video id. */
    val isVideoUrlValid: Boolean
        get() = videoUrl.isBlank() || videoId != null
}
