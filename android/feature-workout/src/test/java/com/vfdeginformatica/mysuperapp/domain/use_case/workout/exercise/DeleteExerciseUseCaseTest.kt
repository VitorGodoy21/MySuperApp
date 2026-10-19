package com.vfdeginformatica.mysuperapp.domain.use_case.workout.exercise

import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.domain.model.Workout
import com.vfdeginformatica.mysuperapp.domain.model.WorkoutItem
import com.vfdeginformatica.mysuperapp.domain.repository.FakeExerciseRepository
import com.vfdeginformatica.mysuperapp.domain.repository.FakeWorkoutRepository
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeleteExerciseUseCaseTest {

    private fun workout(id: String, name: String, vararg exerciseIds: String) = Workout(
        id = id,
        name = name,
        items = exerciseIds.mapIndexed { index, exerciseId -> WorkoutItem(exerciseId, index) },
    )

    @Test
    fun `exercise used in workouts emits error naming them and does not delete`() = runTest {
        val exerciseRepo = FakeExerciseRepository()
        val workoutRepo = FakeWorkoutRepository(
            workouts = listOf(
                workout("w1", "Treino A", "e1", "e2"),
                workout("w2", "Treino B", "e1"),
                workout("w3", "Treino C", "e3"),
            )
        )
        val useCase = DeleteExerciseUseCase(exerciseRepo, workoutRepo)

        val emissions = useCase("e1").toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is Resource.Loading)
        val error = emissions[1] as Resource.Error
        assertTrue(error.message!!.contains("Treino A"))
        assertTrue(error.message!!.contains("Treino B"))
        assertTrue(exerciseRepo.deletedIds.isEmpty())
        assertEquals(listOf("e1"), workoutRepo.usageQueries)
    }

    @Test
    fun `exercise not in use is deleted`() = runTest {
        val exerciseRepo = FakeExerciseRepository()
        val workoutRepo = FakeWorkoutRepository(
            workouts = listOf(workout("w1", "Treino A", "e2"))
        )
        val useCase = DeleteExerciseUseCase(exerciseRepo, workoutRepo)

        val emissions = useCase("e1").toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is Resource.Loading)
        assertTrue(emissions[1] is Resource.Success)
        assertEquals(listOf("e1"), exerciseRepo.deletedIds)
    }

    @Test
    fun `blank id emits error`() = runTest {
        val exerciseRepo = FakeExerciseRepository()
        val useCase = DeleteExerciseUseCase(exerciseRepo, FakeWorkoutRepository())

        val emissions = useCase("  ").toList()

        assertTrue(emissions.last() is Resource.Error)
        assertTrue(exerciseRepo.deletedIds.isEmpty())
    }
}
