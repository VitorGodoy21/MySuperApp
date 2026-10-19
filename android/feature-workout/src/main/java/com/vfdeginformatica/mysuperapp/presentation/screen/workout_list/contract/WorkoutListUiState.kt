package com.vfdeginformatica.mysuperapp.presentation.screen.workout_list.contract

import com.vfdeginformatica.mysuperapp.domain.model.Exercise
import com.vfdeginformatica.mysuperapp.domain.model.Workout

data class WorkoutRow(
    val workout: Workout,
    val dayLabel: String?,
    val exerciseCount: Int,
    val preview: String,
)

data class WorkoutListUiState(
    val isLoading: Boolean = false,
    val workouts: List<Workout> = emptyList(),
    val exercises: List<Exercise> = emptyList(),
    val errorMessage: String? = null,
) {
    val rows: List<WorkoutRow>
        get() {
            val namesById = exercises.associate { it.id to it.name }
            return workouts.map { workout ->
                WorkoutRow(
                    workout = workout,
                    dayLabel = workout.dayOfWeek?.label,
                    exerciseCount = workout.items.size,
                    preview = buildPreview(workout, namesById),
                )
            }
        }

    companion object {
        const val PREVIEW_LIMIT = 3
        const val REMOVED_EXERCISE_LABEL = "Exercício removido"

        internal fun buildPreview(workout: Workout, namesById: Map<String, String>): String {
            val names = workout.items.map { namesById[it.exerciseId] ?: REMOVED_EXERCISE_LABEL }
            val shown = names.take(PREVIEW_LIMIT).joinToString(" • ")
            val remaining = names.size - PREVIEW_LIMIT
            return if (remaining > 0) "$shown +$remaining" else shown
        }
    }
}
