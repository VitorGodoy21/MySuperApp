package com.vfdeginformatica.mysuperapp.data.mapper

import com.vfdeginformatica.mysuperapp.data.remote.dto.WorkoutSessionDto
import com.vfdeginformatica.mysuperapp.domain.model.WorkoutSession
import java.time.Instant
import java.time.LocalDate

fun WorkoutSessionDto.toModel(): WorkoutSession = WorkoutSession(
    id = id,
    workoutId = workoutId,
    date = date?.toLocalDate() ?: createdAt?.toLocalDate() ?: LocalDate.now(),
    durationMinutes = durationMinutes?.takeIf { it > 0 },
    calories = calories?.takeIf { it > 0 },
    createdAt = createdAt?.let { Instant.ofEpochSecond(it.seconds, it.nanoseconds.toLong()) },
)

fun WorkoutSession.toDto(): WorkoutSessionDto = WorkoutSessionDto(
    id = id,
    workoutId = workoutId,
    date = date.toTimestamp(),
    durationMinutes = durationMinutes,
    calories = calories,
)
