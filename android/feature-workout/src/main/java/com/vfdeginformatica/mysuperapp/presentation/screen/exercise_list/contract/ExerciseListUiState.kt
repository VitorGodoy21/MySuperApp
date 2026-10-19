package com.vfdeginformatica.mysuperapp.presentation.screen.exercise_list.contract

import com.vfdeginformatica.mysuperapp.domain.model.Exercise
import com.vfdeginformatica.mysuperapp.domain.model.MuscleGroup

data class ExerciseGroup(
    val muscleGroup: MuscleGroup?,
    val exercises: List<Exercise>,
) {
    val label: String
        get() = muscleGroup?.label ?: "Sem grupo"
}

data class ExerciseListUiState(
    val isLoading: Boolean = false,
    val exercises: List<Exercise> = emptyList(),
    val query: String = "",
    val errorMessage: String? = null,
) {
    val hasQuery: Boolean
        get() = query.isNotBlank()

    val filteredExercises: List<Exercise>
        get() {
            val term = query.trim()
            if (term.isEmpty()) return exercises
            return exercises.filter { it.name.contains(term, ignoreCase = true) }
        }

    /** Grouped by muscle group in [MuscleGroup.entries] order, "Sem grupo" last. */
    val groups: List<ExerciseGroup>
        get() {
            val byGroup = filteredExercises.groupBy { it.muscleGroup }
            val ordered: List<MuscleGroup?> = MuscleGroup.entries + listOf<MuscleGroup?>(null)
            return ordered.mapNotNull { group ->
                byGroup[group]
                    ?.takeIf { it.isNotEmpty() }
                    ?.let { list ->
                        ExerciseGroup(group, list.sortedBy { it.name.lowercase() })
                    }
            }
        }
}
