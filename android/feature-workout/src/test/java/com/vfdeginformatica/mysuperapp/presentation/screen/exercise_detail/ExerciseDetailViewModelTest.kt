package com.vfdeginformatica.mysuperapp.presentation.screen.exercise_detail

import androidx.lifecycle.SavedStateHandle
import com.vfdeginformatica.mysuperapp.domain.model.Exercise
import com.vfdeginformatica.mysuperapp.domain.model.MuscleGroup
import com.vfdeginformatica.mysuperapp.domain.model.Workout
import com.vfdeginformatica.mysuperapp.domain.model.WorkoutItem
import com.vfdeginformatica.mysuperapp.domain.repository.FakeExerciseRepository
import com.vfdeginformatica.mysuperapp.domain.repository.FakeWorkoutRepository
import com.vfdeginformatica.mysuperapp.domain.use_case.workout.exercise.AddExerciseUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.workout.exercise.DeleteExerciseUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.workout.exercise.GetExercisesUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.workout.exercise.GetWorkoutsUsingExerciseUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.workout.exercise.UpdateExerciseUseCase
import com.vfdeginformatica.mysuperapp.presentation.screen.exercise_detail.contract.ExerciseDetailEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ExerciseDetailViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private fun buildViewModel(
        exerciseId: String?,
        exerciseRepository: FakeExerciseRepository = FakeExerciseRepository(),
        workoutRepository: FakeWorkoutRepository = FakeWorkoutRepository(),
    ): ExerciseDetailViewModel {
        val handle = SavedStateHandle(
            if (exerciseId != null) mapOf(ExerciseDetailViewModel.EXERCISE_ID_ARG to exerciseId)
            else emptyMap()
        )
        return ExerciseDetailViewModel(
            savedStateHandle = handle,
            getExercisesUseCase = GetExercisesUseCase(exerciseRepository),
            addExerciseUseCase = AddExerciseUseCase(exerciseRepository),
            updateExerciseUseCase = UpdateExerciseUseCase(exerciseRepository),
            getWorkoutsUsingExerciseUseCase = GetWorkoutsUsingExerciseUseCase(workoutRepository),
            deleteExerciseUseCase = DeleteExerciseUseCase(exerciseRepository, workoutRepository),
        )
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `blank name keeps submit disabled and shows error after typing`() = runTest {
        val vm = buildViewModel(null)
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isSubmitEnabled)
        assertNull(vm.uiState.value.nameError)

        vm.onEvent(ExerciseDetailEvent.NameChanged("   "))
        assertFalse(vm.uiState.value.isSubmitEnabled)
        assertEquals(ExerciseDetailViewModel.NAME_REQUIRED_ERROR, vm.uiState.value.nameError)
    }

    @Test
    fun `save in create mode adds exercise with trimmed name and muscle group`() = runTest {
        val repo = FakeExerciseRepository()
        val vm = buildViewModel(null, repo)
        advanceUntilIdle()

        vm.onEvent(ExerciseDetailEvent.NameChanged("  Supino reto  "))
        vm.onEvent(ExerciseDetailEvent.MuscleGroupChanged(MuscleGroup.CHEST))
        assertTrue(vm.uiState.value.isSubmitEnabled)

        vm.onEvent(ExerciseDetailEvent.SaveClicked)
        advanceUntilIdle()

        assertEquals(1, repo.added.size)
        assertEquals("Supino reto", repo.added.first().name)
        assertEquals(MuscleGroup.CHEST, repo.added.first().muscleGroup)
        assertTrue(repo.updated.isEmpty())
    }

    @Test
    fun `duplicate name shows warning but keeps submit enabled`() = runTest {
        val repo = FakeExerciseRepository(
            exercises = listOf(Exercise(id = "e1", name = "Supino Reto", muscleGroup = MuscleGroup.CHEST))
        )
        val vm = buildViewModel(null, repo)
        advanceUntilIdle()

        vm.onEvent(ExerciseDetailEvent.NameChanged(" supino reto "))

        val state = vm.uiState.value
        assertEquals(ExerciseDetailViewModel.DUPLICATE_WARNING, state.duplicateWarning)
        assertTrue(state.isSubmitEnabled)

        vm.onEvent(ExerciseDetailEvent.NameChanged("Supino inclinado"))
        assertNull(vm.uiState.value.duplicateWarning)
    }

    @Test
    fun `edit mode loads exercise and blocks delete when used in a workout`() = runTest {
        val exerciseRepo = FakeExerciseRepository(
            exercises = listOf(
                Exercise(id = "e1", name = "Agachamento", muscleGroup = MuscleGroup.LEGS, notes = "n")
            )
        )
        val workoutRepo = FakeWorkoutRepository(
            workouts = listOf(
                Workout(id = "w1", name = "Treino Pernas", items = listOf(WorkoutItem("e1", 0))),
                Workout(id = "w2", name = "Treino Peito", items = listOf(WorkoutItem("e9", 0))),
            )
        )
        val vm = buildViewModel("e1", exerciseRepo, workoutRepo)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state.isEditing)
        assertEquals("Agachamento", state.name)
        assertEquals(MuscleGroup.LEGS, state.muscleGroup)
        assertTrue(state.isSubmitEnabled)
        assertNull(state.duplicateWarning)
        assertEquals(listOf("Treino Pernas"), state.usedInWorkouts)
        assertFalse(state.canDelete)

        vm.onEvent(ExerciseDetailEvent.DeleteClicked)
        assertFalse(vm.uiState.value.showDeleteConfirmation)
    }

    @Test
    fun `invalid video url shows error and disables submit until cleared`() = runTest {
        val vm = buildViewModel(null)
        advanceUntilIdle()

        vm.onEvent(ExerciseDetailEvent.NameChanged("Remada"))
        assertTrue(vm.uiState.value.isSubmitEnabled)

        vm.onEvent(ExerciseDetailEvent.VideoUrlChanged("https://vimeo.com/123456"))
        assertEquals(ExerciseDetailViewModel.VIDEO_URL_INVALID_ERROR, vm.uiState.value.videoUrlError)
        assertNull(vm.uiState.value.videoId)
        assertFalse(vm.uiState.value.isSubmitEnabled)

        vm.onEvent(ExerciseDetailEvent.VideoUrlCleared)
        assertNull(vm.uiState.value.videoUrlError)
        assertEquals("", vm.uiState.value.videoUrl)
        assertTrue(vm.uiState.value.isSubmitEnabled)
    }

    @Test
    fun `valid video url is saved in canonical form and blank url is saved empty`() = runTest {
        val repo = FakeExerciseRepository()
        val vm = buildViewModel(null, repo)
        advanceUntilIdle()

        vm.onEvent(ExerciseDetailEvent.NameChanged("Remada"))
        vm.onEvent(ExerciseDetailEvent.VideoUrlChanged("https://youtu.be/dQw4w9WgXcQ?si=abc&t=10"))
        assertNull(vm.uiState.value.videoUrlError)
        assertEquals("dQw4w9WgXcQ", vm.uiState.value.videoId)

        vm.onEvent(ExerciseDetailEvent.SaveClicked)
        advanceUntilIdle()
        assertEquals("https://www.youtube.com/watch?v=dQw4w9WgXcQ", repo.added.single().videoUrl)

        val vm2 = buildViewModel(null, repo)
        advanceUntilIdle()
        vm2.onEvent(ExerciseDetailEvent.NameChanged("Rosca"))
        vm2.onEvent(ExerciseDetailEvent.SaveClicked)
        advanceUntilIdle()
        assertEquals("", repo.added.last().videoUrl)
    }

    @Test
    fun `edit mode loads stored video url`() = runTest {
        val repo = FakeExerciseRepository(
            exercises = listOf(
                Exercise(id = "e1", name = "Remada", videoUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ")
            )
        )
        val vm = buildViewModel("e1", repo)
        advanceUntilIdle()

        assertEquals("https://www.youtube.com/watch?v=dQw4w9WgXcQ", vm.uiState.value.videoUrl)
        assertEquals("dQw4w9WgXcQ", vm.uiState.value.videoId)
        assertTrue(vm.uiState.value.isSubmitEnabled)
    }

    @Test
    fun `edit mode delete flow removes unused exercise after confirmation`() = runTest {
        val exerciseRepo = FakeExerciseRepository(
            exercises = listOf(Exercise(id = "e1", name = "Agachamento"))
        )
        val vm = buildViewModel("e1", exerciseRepo, FakeWorkoutRepository())
        advanceUntilIdle()

        assertTrue(vm.uiState.value.canDelete)
        vm.onEvent(ExerciseDetailEvent.DeleteClicked)
        assertTrue(vm.uiState.value.showDeleteConfirmation)

        vm.onEvent(ExerciseDetailEvent.DeleteConfirmed)
        advanceUntilIdle()

        assertNotNull(exerciseRepo.deletedIds.firstOrNull())
        assertEquals(listOf("e1"), exerciseRepo.deletedIds)
    }
}
