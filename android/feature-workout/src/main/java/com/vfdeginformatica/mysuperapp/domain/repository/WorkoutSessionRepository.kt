package com.vfdeginformatica.mysuperapp.domain.repository

import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.domain.model.WorkoutSession
import java.time.LocalDate

interface WorkoutSessionRepository {
    suspend fun getSessions(workoutId: String): List<WorkoutSession>?
    /** Sessions of every workout dated on or after [from]. */
    suspend fun getSessionsSince(from: LocalDate): List<WorkoutSession>?
    suspend fun addSession(session: WorkoutSession): Resource<String>
    suspend fun deleteSession(id: String): Resource<Unit>
}
