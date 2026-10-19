package com.vfdeginformatica.mysuperapp.data.remote.repository

import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.data.mapper.toDto
import com.vfdeginformatica.mysuperapp.data.mapper.toModel
import com.vfdeginformatica.mysuperapp.data.remote.datasource.ExerciseDao
import com.vfdeginformatica.mysuperapp.domain.model.Exercise
import com.vfdeginformatica.mysuperapp.domain.repository.ExerciseRepository

class ExerciseRepositoryImpl(
    private val dao: ExerciseDao
) : ExerciseRepository {

    override suspend fun getExercises(): List<Exercise>? =
        dao.getExercises()?.map { it.toModel() }

    override suspend fun addExercise(exercise: Exercise): Resource<String> =
        dao.addExercise(exercise.toDto())

    override suspend fun updateExercise(exercise: Exercise): Resource<Unit> =
        dao.updateExercise(exercise.toDto())

    override suspend fun deleteExercise(id: String): Resource<Unit> =
        dao.deleteExercise(id)
}
