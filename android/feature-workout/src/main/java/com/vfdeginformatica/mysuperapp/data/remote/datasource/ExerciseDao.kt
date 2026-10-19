package com.vfdeginformatica.mysuperapp.data.remote.datasource

import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.data.remote.dto.ExerciseDto

interface ExerciseDao {
    suspend fun getExercises(): List<ExerciseDto>?
    suspend fun addExercise(exercise: ExerciseDto): Resource<String>
    suspend fun updateExercise(exercise: ExerciseDto): Resource<Unit>
    suspend fun deleteExercise(id: String): Resource<Unit>
}
