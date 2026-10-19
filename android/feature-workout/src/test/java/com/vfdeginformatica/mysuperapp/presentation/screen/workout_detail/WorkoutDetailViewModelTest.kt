package com.vfdeginformatica.mysuperapp.presentation.screen.workout_detail

import androidx.lifecycle.SavedStateHandle
import com.vfdeginformatica.mysuperapp.domain.model.Exercise
import com.vfdeginformatica.mysuperapp.domain.model.MuscleGroup
import com.vfdeginformatica.mysuperapp.domain.model.WeekDay
import com.vfdeginformatica.mysuperapp.domain.model.Workout
import com.vfdeginformatica.mysuperapp.domain.model.WorkoutItem
import com.vfdeginformatica.mysuperapp.domain.repository.FakeExerciseRepository
import com.vfdeginformatica.mysuperapp.domain.repository.FakeWorkoutRepository
import com.vfdeginformatica.mysuperapp.domain.use_case.workout.exercise.GetExercisesUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.workout.workout.DeleteWorkoutUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.workout.workout.GetWorkoutByIdUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.workout.workout.SaveWorkoutUseCase
import com.vfdeginformatica.mysuperapp.presentation.screen.workout_detail.contract.ItemDraft
import com.vfdeginformatica.mysuperapp.presentation.screen.workout_detail.contract.WorkoutDetailEvent
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutDetailViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private val exercises = listOf(
        Exercise(id = "e1", name = "Supino reto", muscleGroup = MuscleGroup.CHEST),
        Exercise(id = "e2", name = "Agachamento", muscleGroup = MuscleGroup.LEGS),
    )

    private fun buildViewModel(
        workoutId: String? = null,
        workoutRepository: FakeWorkoutRepository = FakeWorkoutRepository(),
        exerciseRepository: FakeExerciseRepository = FakeExerciseRepository(exercises),
    ): WorkoutDetailViewModel {
        val handle = SavedStateHandle(
            if (workoutId != null) mapOf(WorkoutDetailViewModel.WORKOUT_ID_ARG to workoutId)
            else emptyMap()
        )
        return WorkoutDetailViewModel(
            savedStateHandle = handle,
            getWorkoutByIdUseCase = GetWorkoutByIdUseCase(workoutRepository),
            getExercisesUseCase = GetExercisesUseCase(exerciseRepository),
            saveWorkoutUseCase = SaveWorkoutUseCase(workoutRepository),
            deleteWorkoutUseCase = DeleteWorkoutUseCase(workoutRepository),
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
    fun `blank name disables submit`() = runTest {
        val vm = buildViewModel()
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isSubmitEnabled)

        vm.onEvent(WorkoutDetailEvent.NameChanged("   "))
        assertFalse(vm.uiState.value.isSubmitEnabled)

        vm.onEvent(WorkoutDetailEvent.NameChanged("Treino A"))
        assertTrue(vm.uiState.value.isSubmitEnabled)
        assertNull(vm.uiState.value.nameError)

        vm.onEvent(WorkoutDetailEvent.NameChanged(""))
        assertFalse(vm.uiState.value.isSubmitEnabled)
        assertEquals(WorkoutDetailViewModel.NAME_REQUIRED_ERROR, vm.uiState.value.nameError)
    }

    @Test
    fun `moving second item up swaps ids and re-indexes order`() = runTest {
        val vm = buildViewModel()
        advanceUntilIdle()

        vm.onEvent(WorkoutDetailEvent.AddExerciseClicked)
        assertTrue(vm.uiState.value.showExercisePicker)
        vm.onEvent(WorkoutDetailEvent.ExercisePicked("e1"))
        assertFalse(vm.uiState.value.showExercisePicker)
        vm.onEvent(WorkoutDetailEvent.ExercisePicked("e2"))

        vm.onEvent(WorkoutDetailEvent.MoveItemUp(1))

        val items = vm.uiState.value.items
        assertEquals(listOf("e2", "e1"), items.map { it.exerciseId })
        assertEquals(listOf(0, 1), items.map { it.order })
    }

    @Test
    fun `removing first item re-indexes remaining item to order 0`() = runTest {
        val vm = buildViewModel()
        advanceUntilIdle()

        vm.onEvent(WorkoutDetailEvent.ExercisePicked("e1"))
        vm.onEvent(WorkoutDetailEvent.ExercisePicked("e2"))

        vm.onEvent(WorkoutDetailEvent.RemoveItem(0))

        val items = vm.uiState.value.items
        assertEquals(1, items.size)
        assertEquals("e2", items.single().exerciseId)
        assertEquals(0, items.single().order)
    }

    @Test
    fun `confirming item draft writes parsed prescription into item`() = runTest {
        val vm = buildViewModel()
        advanceUntilIdle()

        vm.onEvent(WorkoutDetailEvent.ExercisePicked("e1"))
        vm.onEvent(WorkoutDetailEvent.EditItemClicked(0))
        assertEquals(0, vm.uiState.value.editingItemIndex)
        assertEquals(ItemDraft(), vm.uiState.value.itemDraft)

        vm.onEvent(
            WorkoutDetailEvent.ItemDraftChanged(
                ItemDraft(sets = "3", reps = "10-12", loadKg = "20,5", restSeconds = "60", notes = " foco ")
            )
        )
        vm.onEvent(WorkoutDetailEvent.ItemDraftConfirmed)

        val item = vm.uiState.value.items.single()
        assertEquals(3, item.sets)
        assertEquals("10-12", item.reps)
        assertEquals(20.5, item.loadKg!!, 0.0001)
        assertEquals(60, item.restSeconds)
        assertEquals("foco", item.notes)
        assertNull(vm.uiState.value.editingItemIndex)
        assertNull(vm.uiState.value.itemDraft)
        assertEquals("3 x 10-12 • 20,5 kg • 60 s desc.", vm.uiState.value.itemRows.single().prescription)
    }

    @Test
    fun `save in create mode adds workout with items in current order`() = runTest {
        val repo = FakeWorkoutRepository()
        val vm = buildViewModel(workoutRepository = repo)
        advanceUntilIdle()

        vm.onEvent(WorkoutDetailEvent.NameChanged(" Treino A "))
        vm.onEvent(WorkoutDetailEvent.DayChanged(WeekDay.MONDAY))
        vm.onEvent(WorkoutDetailEvent.ExercisePicked("e1"))
        vm.onEvent(WorkoutDetailEvent.ExercisePicked("e2"))
        vm.onEvent(WorkoutDetailEvent.MoveItemDown(0))
        vm.onEvent(WorkoutDetailEvent.SaveClicked)
        advanceUntilIdle()

        assertEquals(1, repo.added.size)
        val saved = repo.added.single()
        assertEquals("", saved.id)
        assertEquals("Treino A", saved.name)
        assertEquals(WeekDay.MONDAY, saved.dayOfWeek)
        assertEquals(listOf("e2", "e1"), saved.items.map { it.exerciseId })
        assertEquals(listOf(0, 1), saved.items.map { it.order })
        assertTrue(repo.updated.isEmpty())
        assertFalse(vm.uiState.value.isSaving)
    }

    @Test
    fun `edit mode loads workout and save updates it`() = runTest {
        val repo = FakeWorkoutRepository(
            workouts = listOf(
                Workout(
                    id = "w1",
                    name = "Treino B",
                    dayOfWeek = WeekDay.TUESDAY,
                    items = listOf(WorkoutItem("e2", 0, sets = 4), WorkoutItem("e1", 1)),
                    notes = "leve",
                )
            )
        )
        val vm = buildViewModel(workoutId = "w1", workoutRepository = repo)
        advanceUntilIdle()

        val loaded = vm.uiState.value
        assertTrue(loaded.isEditing)
        assertFalse(loaded.isLoading)
        assertEquals("Treino B", loaded.name)
        assertEquals(WeekDay.TUESDAY, loaded.dayOfWeek)
        assertEquals("leve", loaded.notes)
        assertEquals(listOf("e2", "e1"), loaded.items.map { it.exerciseId })
        assertEquals(listOf("Agachamento", "Supino reto"), loaded.itemRows.map { it.exerciseName })
        assertTrue(loaded.isSubmitEnabled)

        vm.onEvent(WorkoutDetailEvent.NameChanged("Treino B2"))
        vm.onEvent(WorkoutDetailEvent.SaveClicked)
        advanceUntilIdle()

        assertEquals(1, repo.updated.size)
        assertEquals("w1", repo.updated.single().id)
        assertEquals("Treino B2", repo.updated.single().name)
        assertEquals(4, repo.updated.single().items.first().sets)
        assertTrue(repo.added.isEmpty())
    }

    @Test
    fun `move up at index 0 is a no-op`() = runTest {
        val vm = buildViewModel()
        advanceUntilIdle()

        vm.onEvent(WorkoutDetailEvent.ExercisePicked("e1"))
        vm.onEvent(WorkoutDetailEvent.ExercisePicked("e2"))
        val before = vm.uiState.value.items

        vm.onEvent(WorkoutDetailEvent.MoveItemUp(0))
        assertEquals(before, vm.uiState.value.items)
        assertFalse(vm.uiState.value.canMoveUp(0))
        assertFalse(vm.uiState.value.canMoveDown(1))

        vm.onEvent(WorkoutDetailEvent.MoveItemDown(1))
        assertEquals(before, vm.uiState.value.items)
    }

    @Test
    fun `delete requires confirmation then calls repository`() = runTest {
        val repo = FakeWorkoutRepository(workouts = listOf(Workout(id = "w1", name = "Treino A")))
        val vm = buildViewModel(workoutId = "w1", workoutRepository = repo)
        advanceUntilIdle()

        vm.onEvent(WorkoutDetailEvent.DeleteClicked)
        assertTrue(vm.uiState.value.showDeleteConfirmation)
        vm.onEvent(WorkoutDetailEvent.DeleteDismissed)
        assertFalse(vm.uiState.value.showDeleteConfirmation)
        assertTrue(repo.deletedIds.isEmpty())

        vm.onEvent(WorkoutDetailEvent.DeleteClicked)
        vm.onEvent(WorkoutDetailEvent.DeleteConfirmed)
        advanceUntilIdle()

        assertEquals(listOf("w1"), repo.deletedIds)
    }
}
