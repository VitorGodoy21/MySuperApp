package com.vfdeginformatica.mysuperapp.domain.repository

import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.domain.model.Workout

interface WorkoutRepository {
    suspend fun getWorkouts(): List<Workout>?
    suspend fun getWorkoutsUsingExercise(exerciseId: String): List<Workout>?
    suspend fun addWorkout(workout: Workout): Resource<String>
    suspend fun updateWorkout(workout: Workout): Resource<Unit>
    suspend fun deleteWorkout(id: String): Resource<Unit>
}
