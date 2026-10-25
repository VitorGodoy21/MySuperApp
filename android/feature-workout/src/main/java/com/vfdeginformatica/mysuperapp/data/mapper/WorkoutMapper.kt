package com.vfdeginformatica.mysuperapp.data.mapper

import com.vfdeginformatica.mysuperapp.data.remote.dto.WorkoutDto
import com.vfdeginformatica.mysuperapp.data.remote.dto.WorkoutItemDto
import com.vfdeginformatica.mysuperapp.domain.model.WeekDay
import com.vfdeginformatica.mysuperapp.domain.model.Workout
import com.vfdeginformatica.mysuperapp.domain.model.WorkoutIcon
import com.vfdeginformatica.mysuperapp.domain.model.WorkoutItem

fun WorkoutItemDto.toModel(): WorkoutItem = WorkoutItem(
    exerciseId = exerciseId,
    order = order,
    sets = sets,
    reps = reps?.takeIf { it.isNotBlank() },
    loadKg = loadKg,
    restSeconds = restSeconds,
    notes = notes,
)

fun WorkoutItem.toDto(): WorkoutItemDto = WorkoutItemDto(
    exerciseId = exerciseId,
    order = order,
    sets = sets,
    reps = reps,
    loadKg = loadKg,
    restSeconds = restSeconds,
    notes = notes,
)

fun WorkoutDto.toModel(): Workout = Workout(
    id = id,
    name = name,
    dayOfWeek = WeekDay.fromName(dayOfWeek),
    items = items.sortedBy { it.order }.mapIndexed { index, dto -> dto.toModel().copy(order = index) },
    notes = notes,
    icon = WorkoutIcon.fromName(icon),
)

/** `order` is re-indexed from list position and `exerciseIds` is always derived from `items`. */
fun Workout.toDto(): WorkoutDto = WorkoutDto(
    id = id,
    name = name,
    dayOfWeek = dayOfWeek?.name,
    items = items.mapIndexed { index, item -> item.copy(order = index).toDto() },
    exerciseIds = exerciseIds,
    notes = notes,
    icon = icon.name,
)
