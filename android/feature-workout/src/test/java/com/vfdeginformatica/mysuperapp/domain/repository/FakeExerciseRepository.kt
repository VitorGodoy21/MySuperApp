package com.vfdeginformatica.mysuperapp.domain.repository

import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.domain.model.Exercise

open class FakeExerciseRepository(
    var exercises: List<Exercise> = emptyList(),
) : ExerciseRepository {
    val added = mutableListOf<Exercise>()
    val updated = mutableListOf<Exercise>()
    val deletedIds = mutableListOf<String>()
    var writeResult: Resource<Unit> = Resource.Success(Unit)

    /** When true, [getExercises] returns null to simulate a read failure. */
    var failReads: Boolean = false

    override suspend fun getExercises(): List<Exercise>? =
        if (failReads) null else exercises

    override suspend fun addExercise(exercise: Exercise): Resource<String> {
        added += exercise
        return when (val result = writeResult) {
            is Resource.Error -> Resource.Error(result.message ?: "error")
            else -> Resource.Success("new-id")
        }
    }

    override suspend fun updateExercise(exercise: Exercise): Resource<Unit> {
        updated += exercise
        return writeResult
    }

    override suspend fun deleteExercise(id: String): Resource<Unit> {
        deletedIds += id
        return writeResult
    }
}
