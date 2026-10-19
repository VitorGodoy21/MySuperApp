package com.vfdeginformatica.mysuperapp.domain.repository

import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.domain.model.WorkoutSession

interface WorkoutSessionRepository {
    suspend fun getSessions(workoutId: String): List<WorkoutSession>?
    suspend fun addSession(session: WorkoutSession): Resource<String>
    suspend fun deleteSession(id: String): Resource<Unit>
}
