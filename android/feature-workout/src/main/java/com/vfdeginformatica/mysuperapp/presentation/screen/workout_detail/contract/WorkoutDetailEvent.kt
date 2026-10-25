package com.vfdeginformatica.mysuperapp.presentation.screen.workout_detail.contract

import com.vfdeginformatica.mysuperapp.domain.model.WeekDay
import com.vfdeginformatica.mysuperapp.domain.model.WorkoutIcon

sealed interface WorkoutDetailEvent {
    data class NameChanged(val name: String) : WorkoutDetailEvent
    data class DayChanged(val day: WeekDay?) : WorkoutDetailEvent
    data class IconChanged(val icon: WorkoutIcon) : WorkoutDetailEvent
    data class NotesChanged(val notes: String) : WorkoutDetailEvent

    data object AddExerciseClicked : WorkoutDetailEvent
    data class PickerQueryChanged(val query: String) : WorkoutDetailEvent
    data class ExercisePicked(val exerciseId: String) : WorkoutDetailEvent
    data object PickerDismissed : WorkoutDetailEvent

    data class RemoveItem(val index: Int) : WorkoutDetailEvent
    data class MoveItemUp(val index: Int) : WorkoutDetailEvent
    data class MoveItemDown(val index: Int) : WorkoutDetailEvent

    data class EditItemClicked(val index: Int) : WorkoutDetailEvent
    data class ItemDraftChanged(val draft: ItemDraft) : WorkoutDetailEvent
    data object ItemDraftConfirmed : WorkoutDetailEvent
    data object ItemDraftDismissed : WorkoutDetailEvent

    data class PlayVideoClicked(val index: Int) : WorkoutDetailEvent
    data object VideoDialogDismissed : WorkoutDetailEvent

    data object SaveClicked : WorkoutDetailEvent
    data object DeleteClicked : WorkoutDetailEvent
    data object DeleteConfirmed : WorkoutDetailEvent
    data object DeleteDismissed : WorkoutDetailEvent
}
