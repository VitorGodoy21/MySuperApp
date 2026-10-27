package com.vfdeginformatica.mysuperapp.domain.model

/**
 * One exercise inside a workout. Prescription fields (sets, reps, load, rest) are optional
 * so a workout can be assembled first and detailed later.
 */
data class WorkoutItem(
    val exerciseId: String,
    val order: Int,
    val sets: Int? = null,
    val reps: String? = null,
    val loadKg: Double? = null,
    val restSeconds: Int? = null,
    val notes: String = "",
)

data class Workout(
    val id: String = "",
    val name: String,
    val dayOfWeek: WeekDay? = null,
    val items: List<WorkoutItem> = emptyList(),
    val notes: String = "",
    val icon: WorkoutIcon = WorkoutIcon.DEFAULT,
    /** Shown in the hub under the collapsible "Outros" group instead of the main list. */
    val isOther: Boolean = false,
) {
    val exerciseIds: List<String>
        get() = items.map { it.exerciseId }.distinct()
}
