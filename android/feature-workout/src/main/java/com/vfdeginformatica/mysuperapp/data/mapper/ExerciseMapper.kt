package com.vfdeginformatica.mysuperapp.data.mapper

import com.vfdeginformatica.mysuperapp.data.remote.dto.ExerciseDto
import com.vfdeginformatica.mysuperapp.domain.model.Exercise
import com.vfdeginformatica.mysuperapp.domain.model.MuscleGroup

fun ExerciseDto.toModel(): Exercise = Exercise(
    id = id,
    name = name,
    muscleGroup = if (muscleGroup.isBlank()) null else MuscleGroup.fromName(muscleGroup),
    description = description,
    notes = notes,
)

fun Exercise.toDto(): ExerciseDto = ExerciseDto(
    id = id,
    name = name,
    muscleGroup = muscleGroup?.name.orEmpty(),
    description = description,
    notes = notes,
)
