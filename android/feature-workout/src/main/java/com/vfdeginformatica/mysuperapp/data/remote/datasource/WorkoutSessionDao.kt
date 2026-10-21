package com.vfdeginformatica.mysuperapp.data.remote.datasource

import com.google.firebase.Timestamp
import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.data.remote.dto.WorkoutSessionDto

interface WorkoutSessionDao {
    suspend fun getSessions(workoutId: String): List<WorkoutSessionDto>?
    /** All sessions (any workout) whose `date` is on or after [from]. */
    suspend fun getSessionsSince(from: Timestamp): List<WorkoutSessionDto>?
    suspend fun addSession(session: WorkoutSessionDto): Resource<String>
    suspend fun deleteSession(id: String): Resource<Unit>
}
