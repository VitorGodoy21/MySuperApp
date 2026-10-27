package com.vfdeginformatica.mysuperapp.presentation.screen.workout_history

import com.vfdeginformatica.mysuperapp.domain.model.Workout
import com.vfdeginformatica.mysuperapp.domain.model.WorkoutSession
import com.vfdeginformatica.mysuperapp.domain.repository.FakeWorkoutRepository
import com.vfdeginformatica.mysuperapp.domain.repository.FakeWorkoutSessionRepository
import com.vfdeginformatica.mysuperapp.domain.use_case.workout.session.DeleteWorkoutSessionUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.workout.session.GetRecentWorkoutSessionsUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.workout.workout.GetWorkoutsUseCase
import com.vfdeginformatica.mysuperapp.presentation.screen.workout_history.contract.WorkoutHistoryEffect
import com.vfdeginformatica.mysuperapp.presentation.screen.workout_history.contract.WorkoutHistoryEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutHistoryViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val today: LocalDate = LocalDate.now()

    private val workouts = listOf(
        Workout(id = "wA", name = "Treino A"),
        Workout(id = "wB", name = "Treino B"),
    )

    private fun session(
        id: String,
        workoutId: String,
        daysAgo: Long,
        createdAtEpoch: Long = 0,
    ) = WorkoutSession(
        id = id,
        workoutId = workoutId,
        date = today.minusDays(daysAgo),
        durationMinutes = 60,
        createdAt = Instant.ofEpochSecond(createdAtEpoch),
    )

    private fun buildViewModel(
        sessionRepository: FakeWorkoutSessionRepository = FakeWorkoutSessionRepository(),
        workoutRepository: FakeWorkoutRepository = FakeWorkoutRepository(workouts),
    ) = WorkoutHistoryViewModel(
        getWorkoutsUseCase = GetWorkoutsUseCase(workoutRepository),
        getRecentWorkoutSessionsUseCase = GetRecentWorkoutSessionsUseCase(sessionRepository),
        deleteWorkoutSessionUseCase = DeleteWorkoutSessionUseCase(sessionRepository),
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `groups recent sessions by day newest first with several per day`() = runTest {
        val repo = FakeWorkoutSessionRepository(
            listOf(
                session("s1", "wA", daysAgo = 4),
                session("s2", "wB", daysAgo = 2),
                session("s3", "wA", daysAgo = 1, createdAtEpoch = 100),
                session("s4", "wB", daysAgo = 1, createdAtEpoch = 200),
                session("old", "wA", daysAgo = 31),
                session("gone", "deleted", daysAgo = 0),
            )
        )
        val vm = buildViewModel(repo)
        vm.onEvent(WorkoutHistoryEvent.Reload)
        advanceUntilIdle()

        val days = vm.uiState.value.historyDays
        assertEquals(listOf(today, today.minusDays(1), today.minusDays(2), today.minusDays(4)), days.map { it.date })

        val sameDay = days[1].entries
        assertEquals(listOf("s4", "s3"), sameDay.map { it.session.id })
        assertEquals(listOf("Treino B", "Treino A"), sameDay.map { it.workoutName })

        val orphan = days[0].entries.single()
        assertNull(orphan.workoutName)
        assertFalse(orphan.workoutExists)
        assertFalse(vm.uiState.value.isSessionsLoading)
    }

    @Test
    fun `tapping a session navigates to its workout or warns when removed`() = runTest {
        val repo = FakeWorkoutSessionRepository(
            listOf(session("s1", "wA", daysAgo = 1), session("gone", "deleted", daysAgo = 0))
        )
        val vm = buildViewModel(repo)
        val effects = mutableListOf<WorkoutHistoryEffect>()
        val job = launch { vm.effect.collect { effects += it } }
        vm.onEvent(WorkoutHistoryEvent.Reload)
        advanceUntilIdle()

        vm.onEvent(WorkoutHistoryEvent.SessionClicked("s1"))
        vm.onEvent(WorkoutHistoryEvent.SessionClicked("gone"))
        advanceUntilIdle()
        job.cancel()

        assertEquals(
            listOf(
                WorkoutHistoryEffect.NavigateToWorkout("wA"),
                WorkoutHistoryEffect.ShowToast(WorkoutHistoryViewModel.WORKOUT_REMOVED_MESSAGE),
            ),
            effects,
        )
    }

    @Test
    fun `deleting a session asks for confirmation then removes it and reloads`() = runTest {
        val repo = FakeWorkoutSessionRepository(listOf(session("s1", "wA", daysAgo = 1)))
        val vm = buildViewModel(repo)
        vm.onEvent(WorkoutHistoryEvent.Reload)
        advanceUntilIdle()

        vm.onEvent(WorkoutHistoryEvent.DeleteSessionClicked("s1"))
        assertEquals("s1", vm.uiState.value.sessionPendingDelete?.id)
        vm.onEvent(WorkoutHistoryEvent.DeleteSessionDismissed)
        assertNull(vm.uiState.value.sessionPendingDelete)
        assertTrue(repo.deletedIds.isEmpty())

        vm.onEvent(WorkoutHistoryEvent.DeleteSessionClicked("s1"))
        vm.onEvent(WorkoutHistoryEvent.DeleteSessionConfirmed)
        advanceUntilIdle()

        assertEquals(listOf("s1"), repo.deletedIds)
        assertTrue(vm.uiState.value.historyDays.isEmpty())
        assertNull(vm.uiState.value.sessionPendingDelete)
        assertFalse(vm.uiState.value.isDeletingSession)
    }
}
