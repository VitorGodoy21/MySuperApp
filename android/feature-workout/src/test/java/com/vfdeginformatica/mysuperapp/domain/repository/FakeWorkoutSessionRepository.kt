package com.vfdeginformatica.mysuperapp.domain.repository

import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.domain.model.WorkoutSession
import java.time.LocalDate

open class FakeWorkoutSessionRepository(
    var sessions: List<WorkoutSession> = emptyList(),
) : WorkoutSessionRepository {
    val added = mutableListOf<WorkoutSession>()
    val deletedIds = mutableListOf<String>()
    var writeResult: Resource<Unit> = Resource.Success(Unit)
    var failReads: Boolean = false

    override suspend fun getSessions(workoutId: String): List<WorkoutSession>? =
        if (failReads) null else sessions.filter { it.workoutId == workoutId }

    override suspend fun getSessionsSince(from: LocalDate): List<WorkoutSession>? =
        if (failReads) null else sessions.filter { !it.date.isBefore(from) }

    override suspend fun addSession(session: WorkoutSession): Resource<String> {
        added += session
        return when (val result = writeResult) {
            is Resource.Error -> Resource.Error(result.message ?: "error")
            else -> {
                val saved = session.copy(id = "s${added.size}")
                sessions = sessions + saved
                Resource.Success(saved.id)
            }
        }
    }

    override suspend fun deleteSession(id: String): Resource<Unit> {
        deletedIds += id
        if (writeResult is Resource.Success) sessions = sessions.filterNot { it.id == id }
        return writeResult
    }
}
