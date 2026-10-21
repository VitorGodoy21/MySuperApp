package com.vfdeginformatica.mysuperapp.domain.model

import java.time.Instant
import java.time.LocalDate

/**
 * One gym visit where a [Workout] was performed. Only the date is mandatory;
 * duration and calories are optional so a session can be logged quickly.
 * [createdAt] is set by the server and only used to order sessions logged on the same day.
 */
data class WorkoutSession(
    val id: String = "",
    val workoutId: String,
    val date: LocalDate,
    val durationMinutes: Int? = null,
    val calories: Int? = null,
    val createdAt: Instant? = null,
)
