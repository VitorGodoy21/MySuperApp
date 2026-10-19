package com.vfdeginformatica.mysuperapp.presentation.screen.workout_detail.contract

import com.vfdeginformatica.mysuperapp.domain.model.Exercise
import com.vfdeginformatica.mysuperapp.domain.model.WeekDay
import com.vfdeginformatica.mysuperapp.domain.model.WorkoutItem
import com.vfdeginformatica.mysuperapp.presentation.common.formatKg

/** Text-backed draft of a [WorkoutItem]'s prescription fields while the edit dialog is open. */
data class ItemDraft(
    val sets: String = "",
    val reps: String = "",
    val loadKg: String = "",
    val restSeconds: String = "",
    val notes: String = "",
)

data class WorkoutItemRow(
    val index: Int,
    val item: WorkoutItem,
    val exerciseName: String,
    val muscleGroupLabel: String?,
    val prescription: String,
    val videoId: String? = null,
)

/** Video currently open in the player dialog. */
data class VideoPlayback(val exerciseName: String, val videoId: String)

data class WorkoutDetailUiState(
    val workoutId: String? = null,
    val isLoading: Boolean = false,
    val name: String = "",
    val dayOfWeek: WeekDay? = null,
    val notes: String = "",
    val items: List<WorkoutItem> = emptyList(),
    val availableExercises: List<Exercise> = emptyList(),
    val showExercisePicker: Boolean = false,
    val pickerQuery: String = "",
    val editingItemIndex: Int? = null,
    val itemDraft: ItemDraft? = null,
    val playingVideo: VideoPlayback? = null,
    val isSaving: Boolean = false,
    val isDeleting: Boolean = false,
    val showDeleteConfirmation: Boolean = false,
    val isSubmitEnabled: Boolean = false,
    val nameError: String? = null,
) {
    val isEditing: Boolean
        get() = !workoutId.isNullOrBlank()

    val isBusy: Boolean
        get() = isSaving || isDeleting

    val itemRows: List<WorkoutItemRow>
        get() {
            val byId = availableExercises.associateBy { it.id }
            return items.mapIndexed { index, item ->
                val exercise = byId[item.exerciseId]
                WorkoutItemRow(
                    index = index,
                    item = item,
                    exerciseName = exercise?.name ?: REMOVED_EXERCISE_LABEL,
                    muscleGroupLabel = exercise?.muscleGroup?.label,
                    prescription = prescriptionOf(item),
                    videoId = exercise?.videoId,
                )
            }
        }

    val pickerExercises: List<Exercise>
        get() {
            val query = pickerQuery.trim()
            if (query.isEmpty()) return availableExercises
            return availableExercises.filter { it.name.contains(query, ignoreCase = true) }
        }

    fun canMoveUp(index: Int): Boolean = index > 0 && index < items.size

    fun canMoveDown(index: Int): Boolean = index >= 0 && index < items.size - 1

    companion object {
        const val REMOVED_EXERCISE_LABEL = "Exercício removido"

        /** e.g. "3 x 10-12 • 20,0 kg • 60 s desc."; empty when nothing is set. */
        internal fun prescriptionOf(item: WorkoutItem): String {
            val pieces = mutableListOf<String>()
            val sets = item.sets
            val reps = item.reps?.takeIf { it.isNotBlank() }
            when {
                sets != null && reps != null -> pieces += "$sets x $reps"
                sets != null -> pieces += "$sets séries"
                reps != null -> pieces += "$reps reps"
            }
            item.loadKg?.let { pieces += formatKg(it) }
            item.restSeconds?.let { pieces += "$it s desc." }
            return pieces.joinToString(" • ")
        }
    }
}
