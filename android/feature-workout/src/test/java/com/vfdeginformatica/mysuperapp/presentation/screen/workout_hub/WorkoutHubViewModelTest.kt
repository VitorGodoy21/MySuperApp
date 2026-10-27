package com.vfdeginformatica.mysuperapp.presentation.screen.workout_hub

import com.vfdeginformatica.mysuperapp.domain.model.Workout
import com.vfdeginformatica.mysuperapp.domain.repository.FakeWorkoutRepository
import com.vfdeginformatica.mysuperapp.domain.use_case.workout.workout.GetWorkoutsUseCase
import com.vfdeginformatica.mysuperapp.presentation.screen.workout_hub.contract.WorkoutHubEffect
import com.vfdeginformatica.mysuperapp.presentation.screen.workout_hub.contract.WorkoutHubEvent
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutHubViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private fun buildViewModel(workouts: List<Workout> = emptyList()) =
        WorkoutHubViewModel(getWorkoutsUseCase = GetWorkoutsUseCase(FakeWorkoutRepository(workouts)))

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `diet, body measurements and history shortcuts navigate without workouts`() = runTest {
        val vm = buildViewModel()
        val effects = mutableListOf<WorkoutHubEffect>()
        val job = launch { vm.effect.collect { effects += it } }
        vm.onEvent(WorkoutHubEvent.Reload)
        advanceUntilIdle()
        vm.onEvent(WorkoutHubEvent.DietClicked)
        vm.onEvent(WorkoutHubEvent.BodyMeasurementsClicked)
        vm.onEvent(WorkoutHubEvent.HistoryClicked)
        advanceUntilIdle()
        job.cancel()
        assertEquals(
            listOf(
                WorkoutHubEffect.NavigateToDiet,
                WorkoutHubEffect.NavigateToBodyMeasurements,
                WorkoutHubEffect.NavigateToHistory,
            ),
            effects,
        )
    }

    @Test
    fun `workouts flagged as other are split out and the group toggles from collapsed`() = runTest {
        val vm = buildViewModel(
            listOf(
                Workout(id = "wA", name = "Treino A"),
                Workout(id = "wX", name = "Extra", isOther = true),
                Workout(id = "wB", name = "Treino B"),
            )
        )
        vm.onEvent(WorkoutHubEvent.Reload)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(listOf("wA", "wB"), state.mainWorkouts.map { it.id })
        assertEquals(listOf("wX"), state.otherWorkouts.map { it.id })
        assertFalse(state.isOthersExpanded)

        vm.onEvent(WorkoutHubEvent.OthersToggled)
        assertTrue(vm.uiState.value.isOthersExpanded)
        vm.onEvent(WorkoutHubEvent.OthersToggled)
        assertFalse(vm.uiState.value.isOthersExpanded)
    }

    @Test
    fun `tapping a workout opens its view screen`() = runTest {
        val vm = buildViewModel(listOf(Workout(id = "wA", name = "Treino A")))
        val effects = mutableListOf<WorkoutHubEffect>()
        val job = launch { vm.effect.collect { effects += it } }
        vm.onEvent(WorkoutHubEvent.Reload)
        advanceUntilIdle()
        assertEquals(listOf("wA"), vm.uiState.value.workouts.map { it.id })
        vm.onEvent(WorkoutHubEvent.WorkoutClicked("wA"))
        advanceUntilIdle()
        job.cancel()
        assertEquals(listOf(WorkoutHubEffect.NavigateToWorkout("wA")), effects)
    }
}
