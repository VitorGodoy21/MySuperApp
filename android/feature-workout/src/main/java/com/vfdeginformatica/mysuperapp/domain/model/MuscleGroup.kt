package com.vfdeginformatica.mysuperapp.domain.model

enum class MuscleGroup(val label: String) {
    CHEST("Peito"),
    BACK("Costas"),
    LEGS("Pernas"),
    SHOULDERS("Ombros"),
    BICEPS("Bíceps"),
    TRICEPS("Tríceps"),
    ABS("Abdômen"),
    GLUTES("Glúteos"),
    CARDIO("Cardio"),
    OTHER("Outro");

    companion object {
        /** Unknown or blank values fall back to [OTHER]. */
        fun fromName(name: String?): MuscleGroup =
            entries.firstOrNull { it.name == name } ?: OTHER
    }
}
