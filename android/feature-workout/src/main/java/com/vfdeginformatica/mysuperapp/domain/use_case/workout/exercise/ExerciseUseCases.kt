package com.vfdeginformatica.mysuperapp.domain.use_case.workout.exercise

import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.domain.model.Exercise
import com.vfdeginformatica.mysuperapp.domain.model.Workout
import com.vfdeginformatica.mysuperapp.domain.repository.ExerciseRepository
import com.vfdeginformatica.mysuperapp.domain.repository.WorkoutRepository
import com.vfdeginformatica.mysuperapp.domain.use_case.workout.readFlow
import com.vfdeginformatica.mysuperapp.domain.use_case.workout.writeFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class GetExercisesUseCase @Inject constructor(
    private val repository: ExerciseRepository,
) {
    /** Sorted by name (pt-BR, case-insensitive). */
    operator fun invoke(): Flow<Resource<List<Exercise>>> =
        readFlow("Erro ao carregar exercícios") {
            repository.getExercises()?.sortedBy { it.name.lowercase() }
        }
}

class AddExerciseUseCase @Inject constructor(
    private val repository: ExerciseRepository,
) {
    operator fun invoke(exercise: Exercise): Flow<Resource<String>> = flow {
        if (exercise.name.isBlank()) {
            emit(Resource.Error("Informe o nome do exercício")); return@flow
        }
        writeFlow("Erro ao salvar exercício") { repository.addExercise(exercise) }
            .collect { emit(it) }
    }
}

class UpdateExerciseUseCase @Inject constructor(
    private val repository: ExerciseRepository,
) {
    operator fun invoke(exercise: Exercise): Flow<Resource<Unit>> = flow {
        when {
            exercise.id.isBlank() -> emit(Resource.Error("Exercício inválido"))
            exercise.name.isBlank() -> emit(Resource.Error("Informe o nome do exercício"))
            else -> writeFlow("Erro ao atualizar exercício") { repository.updateExercise(exercise) }
                .collect { emit(it) }
        }
    }
}

class GetWorkoutsUsingExerciseUseCase @Inject constructor(
    private val workoutRepository: WorkoutRepository,
) {
    operator fun invoke(exerciseId: String): Flow<Resource<List<Workout>>> =
        readFlow("Erro ao verificar treinos") {
            workoutRepository.getWorkoutsUsingExercise(exerciseId)
        }
}

/**
 * Deletes an exercise only when no workout references it. When in use, emits an
 * [Resource.Error] naming the workouts so the UI can explain why deletion was blocked.
 */
class DeleteExerciseUseCase @Inject constructor(
    private val exerciseRepository: ExerciseRepository,
    private val workoutRepository: WorkoutRepository,
) {
    operator fun invoke(exerciseId: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        if (exerciseId.isBlank()) {
            emit(Resource.Error("Exercício inválido")); return@flow
        }
        val usedIn = workoutRepository.getWorkoutsUsingExercise(exerciseId)
        if (usedIn == null) {
            emit(Resource.Error("Erro ao verificar treinos que usam o exercício")); return@flow
        }
        if (usedIn.isNotEmpty()) {
            emit(Resource.Error(inUseMessage(usedIn))); return@flow
        }
        when (val result = exerciseRepository.deleteExercise(exerciseId)) {
            is Resource.Success -> emit(Resource.Success(Unit))
            is Resource.Error -> emit(Resource.Error(result.message ?: "Erro ao excluir exercício"))
            is Resource.Loading -> emit(Resource.Loading())
        }
    }

    companion object {
        fun inUseMessage(workouts: List<Workout>): String {
            val names = workouts.map { it.name }.sorted().joinToString(", ")
            return "Não é possível excluir: o exercício está em uso nos treinos $names. " +
                "Remova-o dos treinos antes de excluir."
        }
    }
}
