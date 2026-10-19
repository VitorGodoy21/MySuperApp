package com.vfdeginformatica.mysuperapp.domain.model

import java.time.LocalDate

/**
 * One gym visit where a [Workout] was performed. Only the date is mandatory;
 * duration and calories are optional so a session can be logged quickly.
 */
data class WorkoutSession(
    val id: String = "",
    val workoutId: String,
    val date: LocalDate,
    val durationMinutes: Int? = null,
    val calories: Int? = null,
)
