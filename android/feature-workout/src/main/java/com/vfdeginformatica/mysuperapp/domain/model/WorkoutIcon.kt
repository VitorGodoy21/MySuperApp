package com.vfdeginformatica.mysuperapp.domain.model

/**
 * Predefined icon a workout can display. Persisted by enum name; the Compose vector for each
 * entry lives in `presentation/common/WorkoutIcons.kt` so the domain stays Android-free.
 */
enum class WorkoutIcon(val label: String) {
    DUMBBELL("Musculação"),
    CARDIO("Cardio"),
    RUNNING("Corrida"),
    TREADMILL("Esteira"),
    BIKE("Bicicleta"),
    SOCCER("Futebol"),
    FIGHT("Luta"),
    MARTIAL_ARTS("Artes marciais"),
    SWIMMING("Natação"),
    YOGA("Yoga / Alongamento"),
    FUNCTIONAL("Funcional"),
    HIIT("HIIT"),
    BASKETBALL("Basquete"),
    VOLLEYBALL("Vôlei"),
    TENNIS("Tênis"),
    HIKING("Caminhada / Trilha"),
    ROWING("Remo");

    companion object {
        val DEFAULT = DUMBBELL

        /** Unknown, null or blank values (e.g. legacy workouts) fall back to [DEFAULT]. */
        fun fromName(name: String?): WorkoutIcon =
            entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}
