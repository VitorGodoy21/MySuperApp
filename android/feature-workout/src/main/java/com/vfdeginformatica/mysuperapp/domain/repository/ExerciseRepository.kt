package com.vfdeginformatica.mysuperapp.domain.repository

import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.domain.model.Exercise

interface ExerciseRepository {
    suspend fun getExercises(): List<Exercise>?
    suspend fun addExercise(exercise: Exercise): Resource<String>
    suspend fun updateExercise(exercise: Exercise): Resource<Unit>
    suspend fun deleteExercise(id: String): Resource<Unit>
}
