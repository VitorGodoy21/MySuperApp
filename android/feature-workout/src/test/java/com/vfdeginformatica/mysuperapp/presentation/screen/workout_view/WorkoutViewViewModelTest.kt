package com.vfdeginformatica.mysuperapp.presentation.screen.workout_view

import androidx.lifecycle.SavedStateHandle
import com.vfdeginformatica.mysuperapp.domain.model.Exercise
import com.vfdeginformatica.mysuperapp.domain.model.MuscleGroup
import com.vfdeginformatica.mysuperapp.domain.model.Workout
import com.vfdeginformatica.mysuperapp.domain.model.WorkoutItem
import com.vfdeginformatica.mysuperapp.domain.model.WorkoutSession
import com.vfdeginformatica.mysuperapp.domain.repository.FakeExerciseRepository
import com.vfdeginformatica.mysuperapp.domain.repository.FakeWorkoutRepository
import com.vfdeginformatica.mysuperapp.domain.repository.FakeWorkoutSessionRepository
import com.vfdeginformatica.mysuperapp.domain.use_case.workout.exercise.GetExercisesUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.workout.session.AddWorkoutSessionUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.workout.session.DeleteWorkoutSessionUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.workout.session.GetWorkoutSessionsUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.workout.workout.GetWorkoutByIdUseCase
import com.vfdeginformatica.mysuperapp.presentation.screen.workout_detail.contract.VideoPlayback
import com.vfdeginformatica.mysuperapp.presentation.screen.workout_view.contract.WorkoutViewEvent
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
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutViewViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private val exercises = listOf(
        Exercise(
            id = "e1",
            name = "Supino reto",
            muscleGroup = MuscleGroup.CHEST,
            videoUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
        ),
        Exercise(id = "e2", name = "Agachamento", muscleGroup = MuscleGroup.LEGS),
    )

    private val workout = Workout(
        id = "w1",
        name = "Treino A",
        items = listOf(WorkoutItem("e1", 0, sets = 3, reps = "10"), WorkoutItem("e2", 1)),
    )

    private fun buildViewModel(
        sessionRepository: FakeWorkoutSessionRepository = FakeWorkoutSessionRepository(),
        workoutId: String = "w1",
    ): WorkoutViewViewModel {
        val workoutRepository = FakeWorkoutRepository(listOf(workout))
        return WorkoutViewViewModel(
            savedStateHandle = SavedStateHandle(mapOf(WorkoutViewViewModel.WORKOUT_ID_ARG to workoutId)),
            getWorkoutByIdUseCase = GetWorkoutByIdUseCase(workoutRepository),
            getExercisesUseCase = GetExercisesUseCase(FakeExerciseRepository(exercises)),
            getWorkoutSessionsUseCase = GetWorkoutSessionsUseCase(sessionRepository),
            addWorkoutSessionUseCase = AddWorkoutSessionUseCase(sessionRepository),
            deleteWorkoutSessionUseCase = DeleteWorkoutSessionUseCase(sessionRepository),
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
    fun `loads workout rows with prescription and video and plays video`() = runTest {
        val vm = buildViewModel()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Treino A", state.workout?.name)
        val rows = state.itemRows
        assertEquals(listOf("Supino reto", "Agachamento"), rows.map { it.exerciseName })
        assertEquals("3 x 10", rows[0].prescription)
        assertEquals("dQw4w9WgXcQ", rows[0].videoId)
        assertNull(rows[1].videoId)

        vm.onEvent(WorkoutViewEvent.PlayVideoClicked(1))
        assertNull(vm.uiState.value.playingVideo)
        vm.onEvent(WorkoutViewEvent.PlayVideoClicked(0))
        assertEquals(VideoPlayback("Supino reto", "dQw4w9WgXcQ"), vm.uiState.value.playingVideo)
        vm.onEvent(WorkoutViewEvent.VideoDialogDismissed)
        assertNull(vm.uiState.value.playingVideo)
    }

    @Test
    fun `sessions are listed newest first and only for this workout`() = runTest {
        val repo = FakeWorkoutSessionRepository(
            listOf(
                WorkoutSession("a", "w1", LocalDate.of(2026, 10, 1), durationMinutes = 60, calories = 300),
                WorkoutSession("b", "w1", LocalDate.of(2026, 10, 10), durationMinutes = 30),
                WorkoutSession("c", "other", LocalDate.of(2026, 10, 15)),
            )
        )
        val vm = buildViewModel(repo)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(listOf("b", "a"), state.sessions.map { it.id })
        assertEquals("b", state.lastSession?.id)
        assertEquals(90, state.totalMinutes)
        assertEquals(300, state.totalCalories)
    }

    @Test
    fun `register dialog validates optional stats and saves only valid values`() = runTest {
        val repo = FakeWorkoutSessionRepository()
        val vm = buildViewModel(repo)
        advanceUntilIdle()

        vm.onEvent(WorkoutViewEvent.RegisterSessionClicked)
        val draft = vm.uiState.value.sessionDraft
        assertNotNull(draft)
        assertEquals(LocalDate.now(), draft!!.date)
        assertTrue(draft.isValid)

        vm.onEvent(WorkoutViewEvent.SessionDurationChanged("0"))
        assertEquals(WorkoutViewViewModel.DURATION_INVALID_ERROR, vm.uiState.value.sessionDraft?.durationError)
        assertFalse(vm.uiState.value.sessionDraft!!.isValid)
        vm.onEvent(WorkoutViewEvent.SessionSaveClicked)
        advanceUntilIdle()
        assertTrue(repo.added.isEmpty())

        vm.onEvent(WorkoutViewEvent.SessionDurationChanged("1h"))
        assertEquals("1", vm.uiState.value.sessionDraft?.durationText)
        vm.onEvent(WorkoutViewEvent.SessionDurationChanged("45"))
        vm.onEvent(WorkoutViewEvent.SessionCaloriesChanged("99999"))
        assertEquals("9999", vm.uiState.value.sessionDraft?.caloriesText)
        assertEquals(WorkoutViewViewModel.CALORIES_INVALID_ERROR, vm.uiState.value.sessionDraft?.caloriesError)
        vm.onEvent(WorkoutViewEvent.SessionCaloriesChanged(""))
        vm.onEvent(WorkoutViewEvent.SessionDateChanged(LocalDate.of(2026, 10, 18)))
        assertTrue(vm.uiState.value.sessionDraft!!.isValid)

        vm.onEvent(WorkoutViewEvent.SessionSaveClicked)
        advanceUntilIdle()

        assertEquals(1, repo.added.size)
        val saved = repo.added.single()
        assertEquals("w1", saved.workoutId)
        assertEquals(LocalDate.of(2026, 10, 18), saved.date)
        assertEquals(45, saved.durationMinutes)
        assertNull(saved.calories)
        assertNull(vm.uiState.value.sessionDraft)
        assertEquals(listOf("s1"), vm.uiState.value.sessions.map { it.id })
    }

    @Test
    fun `future date blocks registering`() = runTest {
        val vm = buildViewModel()
        advanceUntilIdle()

        vm.onEvent(WorkoutViewEvent.RegisterSessionClicked)
        vm.onEvent(WorkoutViewEvent.SessionDateChanged(LocalDate.now().plusDays(1)))

        assertEquals(WorkoutViewViewModel.DATE_FUTURE_ERROR, vm.uiState.value.sessionDraft?.dateError)
        assertFalse(vm.uiState.value.sessionDraft!!.isValid)
    }

    @Test
    fun `deleting a session asks for confirmation then removes it`() = runTest {
        val repo = FakeWorkoutSessionRepository(
            listOf(WorkoutSession("a", "w1", LocalDate.of(2026, 10, 1)))
        )
        val vm = buildViewModel(repo)
        advanceUntilIdle()

        vm.onEvent(WorkoutViewEvent.DeleteSessionClicked("a"))
        assertEquals("a", vm.uiState.value.sessionPendingDelete?.id)
        vm.onEvent(WorkoutViewEvent.DeleteSessionDismissed)
        assertNull(vm.uiState.value.sessionPendingDelete)
        assertTrue(repo.deletedIds.isEmpty())

        vm.onEvent(WorkoutViewEvent.DeleteSessionClicked("a"))
        vm.onEvent(WorkoutViewEvent.DeleteSessionConfirmed)
        advanceUntilIdle()

        assertEquals(listOf("a"), repo.deletedIds)
        assertTrue(vm.uiState.value.sessions.isEmpty())
        assertNull(vm.uiState.value.sessionPendingDelete)
    }
}
