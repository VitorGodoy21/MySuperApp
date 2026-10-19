package com.vfdeginformatica.mysuperapp.data.mapper

import com.vfdeginformatica.mysuperapp.data.remote.dto.WorkoutSessionDto
import com.vfdeginformatica.mysuperapp.domain.model.WorkoutSession
import java.time.LocalDate

fun WorkoutSessionDto.toModel(): WorkoutSession = WorkoutSession(
    id = id,
    workoutId = workoutId,
    date = date?.toLocalDate() ?: createdAt?.toLocalDate() ?: LocalDate.now(),
    durationMinutes = durationMinutes?.takeIf { it > 0 },
    calories = calories?.takeIf { it > 0 },
)

fun WorkoutSession.toDto(): WorkoutSessionDto = WorkoutSessionDto(
    id = id,
    workoutId = workoutId,
    date = date.toTimestamp(),
    durationMinutes = durationMinutes,
    calories = calories,
)
