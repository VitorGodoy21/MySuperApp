package com.vfdeginformatica.mysuperapp.data.remote.repository

import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.data.mapper.toDto
import com.vfdeginformatica.mysuperapp.data.mapper.toModel
import com.vfdeginformatica.mysuperapp.data.remote.datasource.WorkoutDao
import com.vfdeginformatica.mysuperapp.domain.model.Workout
import com.vfdeginformatica.mysuperapp.domain.repository.WorkoutRepository

class WorkoutRepositoryImpl(
    private val dao: WorkoutDao
) : WorkoutRepository {

    override suspend fun getWorkouts(): List<Workout>? =
        dao.getWorkouts()?.map { it.toModel() }

    override suspend fun getWorkoutsUsingExercise(exerciseId: String): List<Workout>? =
        dao.getWorkoutsUsingExercise(exerciseId)?.map { it.toModel() }

    override suspend fun addWorkout(workout: Workout): Resource<String> =
        dao.addWorkout(workout.toDto())

    override suspend fun updateWorkout(workout: Workout): Resource<Unit> =
        dao.updateWorkout(workout.toDto())

    override suspend fun deleteWorkout(id: String): Resource<Unit> =
        dao.deleteWorkout(id)
}
