package com.vfdeginformatica.mysuperapp.domain.use_case.workout.session

import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.domain.model.WorkoutSession
import com.vfdeginformatica.mysuperapp.domain.repository.WorkoutSessionRepository
import com.vfdeginformatica.mysuperapp.domain.use_case.workout.readFlow
import com.vfdeginformatica.mysuperapp.domain.use_case.workout.writeFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.LocalDate
import javax.inject.Inject

class GetWorkoutSessionsUseCase @Inject constructor(
    private val repository: WorkoutSessionRepository,
) {
    /** Newest first. */
    operator fun invoke(workoutId: String): Flow<Resource<List<WorkoutSession>>> =
        readFlow("Erro ao carregar treinos registrados") {
            repository.getSessions(workoutId)?.sortedByDescending { it.date }
        }
}

class AddWorkoutSessionUseCase @Inject constructor(
    private val repository: WorkoutSessionRepository,
) {
    operator fun invoke(session: WorkoutSession): Flow<Resource<String>> = flow {
        validateSession(session)?.let { emit(Resource.Error(it)); return@flow }
        writeFlow("Erro ao registrar treino") { repository.addSession(session) }
            .collect { emit(it) }
    }
}

class DeleteWorkoutSessionUseCase @Inject constructor(
    private val repository: WorkoutSessionRepository,
) {
    operator fun invoke(id: String): Flow<Resource<Unit>> = flow {
        if (id.isBlank()) {
            emit(Resource.Error("Registro inválido")); return@flow
        }
        writeFlow("Erro ao excluir registro") { repository.deleteSession(id) }
            .collect { emit(it) }
    }
}

internal fun validateSession(session: WorkoutSession): String? = when {
    session.workoutId.isBlank() -> "Treino inválido"
    session.date.isAfter(LocalDate.now()) -> "Data não pode ser futura"
    session.durationMinutes != null && session.durationMinutes <= 0 -> "Tempo deve ser maior que zero"
    session.calories != null && session.calories <= 0 -> "Calorias devem ser maiores que zero"
    else -> null
}
