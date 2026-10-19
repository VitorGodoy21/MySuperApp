package com.vfdeginformatica.mysuperapp.presentation.screen.workout_view.contract

import com.vfdeginformatica.mysuperapp.domain.model.Exercise
import com.vfdeginformatica.mysuperapp.domain.model.Workout
import com.vfdeginformatica.mysuperapp.domain.model.WorkoutSession
import com.vfdeginformatica.mysuperapp.presentation.screen.workout_detail.contract.ItemDraft
import com.vfdeginformatica.mysuperapp.presentation.screen.workout_detail.contract.VideoPlayback
import com.vfdeginformatica.mysuperapp.presentation.screen.workout_detail.contract.WorkoutDetailUiState
import com.vfdeginformatica.mysuperapp.presentation.screen.workout_detail.contract.WorkoutItemRow
import java.time.LocalDate

/** Text-backed form for the "Registrar treino" dialog. */
data class SessionDraft(
    val date: LocalDate = LocalDate.now(),
    val durationText: String = "",
    val caloriesText: String = "",
    val dateError: String? = null,
    val durationError: String? = null,
    val caloriesError: String? = null,
    val showDatePicker: Boolean = false,
) {
    val isValid: Boolean
        get() = dateError == null && durationError == null && caloriesError == null
}

data class WorkoutViewUiState(
    val workoutId: String,
    val isLoading: Boolean = false,
    val workout: Workout? = null,
    val availableExercises: List<Exercise> = emptyList(),
    val sessions: List<WorkoutSession> = emptyList(),
    val isSessionsLoading: Boolean = false,
    val playingVideo: VideoPlayback? = null,
    val sessionDraft: SessionDraft? = null,
    val isSavingSession: Boolean = false,
    val sessionPendingDelete: WorkoutSession? = null,
    val isDeletingSession: Boolean = false,
    val editingItemIndex: Int? = null,
    val itemDraft: ItemDraft? = null,
    val isSavingItem: Boolean = false,
) {
    val isBusy: Boolean
        get() = isSavingSession || isDeletingSession || isSavingItem

    val itemRows: List<WorkoutItemRow>
        get() {
            val items = workout?.items.orEmpty()
            val byId = availableExercises.associateBy { it.id }
            return items.mapIndexed { index, item ->
                val exercise = byId[item.exerciseId]
                WorkoutItemRow(
                    index = index,
                    item = item,
                    exerciseName = exercise?.name ?: WorkoutDetailUiState.REMOVED_EXERCISE_LABEL,
                    muscleGroupLabel = exercise?.muscleGroup?.label,
                    prescription = WorkoutDetailUiState.prescriptionOf(item),
                    videoId = exercise?.videoId,
                )
            }
        }

    val lastSession: WorkoutSession?
        get() = sessions.firstOrNull()

    val totalMinutes: Int
        get() = sessions.sumOf { it.durationMinutes ?: 0 }

    val totalCalories: Int
        get() = sessions.sumOf { it.calories ?: 0 }
}

sealed interface WorkoutViewEvent {
    data object Reload : WorkoutViewEvent
    data class PlayVideoClicked(val index: Int) : WorkoutViewEvent
    data object VideoDialogDismissed : WorkoutViewEvent

    data class ItemClicked(val index: Int) : WorkoutViewEvent
    data class ItemDraftChanged(val draft: ItemDraft) : WorkoutViewEvent
    data object ItemDraftConfirmed : WorkoutViewEvent
    data object ItemDraftDismissed : WorkoutViewEvent

    data object RegisterSessionClicked : WorkoutViewEvent
    data object SessionDialogDismissed : WorkoutViewEvent
    data class SessionDateChanged(val date: LocalDate) : WorkoutViewEvent
    data class SessionDatePickerToggled(val visible: Boolean) : WorkoutViewEvent
    data class SessionDurationChanged(val text: String) : WorkoutViewEvent
    data class SessionCaloriesChanged(val text: String) : WorkoutViewEvent
    data object SessionSaveClicked : WorkoutViewEvent

    data class DeleteSessionClicked(val sessionId: String) : WorkoutViewEvent
    data object DeleteSessionDismissed : WorkoutViewEvent
    data object DeleteSessionConfirmed : WorkoutViewEvent
}

sealed interface WorkoutViewEffect {
    data class ShowToast(val message: String) : WorkoutViewEffect
    data object NavigateUp : WorkoutViewEffect
}
