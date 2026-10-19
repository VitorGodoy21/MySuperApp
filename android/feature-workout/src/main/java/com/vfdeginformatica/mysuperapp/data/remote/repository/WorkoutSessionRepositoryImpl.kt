package com.vfdeginformatica.mysuperapp.data.remote.repository

import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.data.mapper.toDto
import com.vfdeginformatica.mysuperapp.data.mapper.toModel
import com.vfdeginformatica.mysuperapp.data.remote.datasource.WorkoutSessionDao
import com.vfdeginformatica.mysuperapp.domain.model.WorkoutSession
import com.vfdeginformatica.mysuperapp.domain.repository.WorkoutSessionRepository

class WorkoutSessionRepositoryImpl(
    private val dao: WorkoutSessionDao
) : WorkoutSessionRepository {

    override suspend fun getSessions(workoutId: String): List<WorkoutSession>? =
        dao.getSessions(workoutId)?.map { it.toModel() }

    override suspend fun addSession(session: WorkoutSession): Resource<String> =
        dao.addSession(session.toDto())

    override suspend fun deleteSession(id: String): Resource<Unit> =
        dao.deleteSession(id)
}
