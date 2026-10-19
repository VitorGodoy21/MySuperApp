package com.vfdeginformatica.mysuperapp.domain.repository

import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.domain.model.Workout

open class FakeWorkoutRepository(
    var workouts: List<Workout> = emptyList(),
) : WorkoutRepository {
    val added = mutableListOf<Workout>()
    val updated = mutableListOf<Workout>()
    val deletedIds = mutableListOf<String>()
    val usageQueries = mutableListOf<String>()
    var writeResult: Resource<Unit> = Resource.Success(Unit)

    /** When true, reads return null to simulate a failure. */
    var failReads: Boolean = false

    override suspend fun getWorkouts(): List<Workout>? =
        if (failReads) null else workouts

    override suspend fun getWorkoutsUsingExercise(exerciseId: String): List<Workout>? {
        usageQueries += exerciseId
        if (failReads) return null
        return workouts.filter { workout -> workout.items.any { it.exerciseId == exerciseId } }
    }

    override suspend fun addWorkout(workout: Workout): Resource<String> {
        added += workout
        return when (val result = writeResult) {
            is Resource.Error -> Resource.Error(result.message ?: "error")
            else -> Resource.Success("new-id")
        }
    }

    override suspend fun updateWorkout(workout: Workout): Resource<Unit> {
        updated += workout
        return writeResult
    }

    override suspend fun deleteWorkout(id: String): Resource<Unit> {
        deletedIds += id
        return writeResult
    }
}
