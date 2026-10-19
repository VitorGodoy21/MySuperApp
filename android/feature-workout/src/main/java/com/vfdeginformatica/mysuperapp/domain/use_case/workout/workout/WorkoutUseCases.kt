package com.vfdeginformatica.mysuperapp.domain.use_case.workout.workout

import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.domain.model.Workout
import com.vfdeginformatica.mysuperapp.domain.repository.WorkoutRepository
import com.vfdeginformatica.mysuperapp.domain.use_case.workout.readFlow
import com.vfdeginformatica.mysuperapp.domain.use_case.workout.writeFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class GetWorkoutsUseCase @Inject constructor(
    private val repository: WorkoutRepository,
) {
    /** Ordered by week day (unscheduled last) then name. */
    operator fun invoke(): Flow<Resource<List<Workout>>> =
        readFlow("Erro ao carregar treinos") {
            repository.getWorkouts()?.sortedWith(
                compareBy<Workout> { it.dayOfWeek?.ordinal ?: Int.MAX_VALUE }
                    .thenBy { it.name.lowercase() }
            )
        }
}

class GetWorkoutByIdUseCase @Inject constructor(
    private val repository: WorkoutRepository,
) {
    operator fun invoke(workoutId: String): Flow<Resource<Workout>> = flow {
        emit(Resource.Loading())
        val workouts = repository.getWorkouts()
        when {
            workouts == null -> emit(Resource.Error("Erro ao carregar treino"))
            else -> workouts.find { it.id == workoutId }
                ?.let { emit(Resource.Success(it)) }
                ?: emit(Resource.Error("Treino não encontrado"))
        }
    }
}

/** Adds when [Workout.id] is blank, otherwise updates. Emits the resulting id. */
class SaveWorkoutUseCase @Inject constructor(
    private val repository: WorkoutRepository,
) {
    operator fun invoke(workout: Workout): Flow<Resource<String>> = flow {
        if (workout.name.isBlank()) {
            emit(Resource.Error("Informe o nome do treino")); return@flow
        }
        if (workout.items.any { it.exerciseId.isBlank() }) {
            emit(Resource.Error("Treino contém item sem exercício")); return@flow
        }
        val normalized = workout.copy(
            items = workout.items.mapIndexed { index, item -> item.copy(order = index) }
        )
        if (normalized.id.isBlank()) {
            writeFlow("Erro ao salvar treino") { repository.addWorkout(normalized) }
                .collect { emit(it) }
        } else {
            writeFlow<Unit>("Erro ao atualizar treino") { repository.updateWorkout(normalized) }
                .collect { result ->
                    when (result) {
                        is Resource.Loading -> emit(Resource.Loading())
                        is Resource.Success -> emit(Resource.Success(normalized.id))
                        is Resource.Error -> emit(Resource.Error(result.message ?: "Erro ao atualizar treino"))
                    }
                }
        }
    }
}

class DeleteWorkoutUseCase @Inject constructor(
    private val repository: WorkoutRepository,
) {
    operator fun invoke(id: String): Flow<Resource<Unit>> = flow {
        if (id.isBlank()) {
            emit(Resource.Error("Treino inválido")); return@flow
        }
        writeFlow("Erro ao excluir treino") { repository.deleteWorkout(id) }
            .collect { emit(it) }
    }
}
