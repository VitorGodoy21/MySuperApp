package com.vfdeginformatica.mysuperapp.data.remote.datasource

import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.data.remote.dto.WorkoutDto

interface WorkoutDao {
    suspend fun getWorkouts(): List<WorkoutDto>?
    suspend fun getWorkoutsUsingExercise(exerciseId: String): List<WorkoutDto>?
    suspend fun addWorkout(workout: WorkoutDto): Resource<String>
    suspend fun updateWorkout(workout: WorkoutDto): Resource<Unit>
    suspend fun deleteWorkout(id: String): Resource<Unit>
}
