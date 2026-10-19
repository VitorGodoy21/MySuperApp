package com.vfdeginformatica.mysuperapp.presentation.screen.body_measurement_detail

import androidx.lifecycle.SavedStateHandle
import com.vfdeginformatica.mysuperapp.domain.model.BodyMeasurement
import com.vfdeginformatica.mysuperapp.domain.repository.FakeBodyMeasurementRepository
import com.vfdeginformatica.mysuperapp.domain.use_case.workout.body_measurement.AddBodyMeasurementUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.workout.body_measurement.DeleteBodyMeasurementUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.workout.body_measurement.GetBodyMeasurementsUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.workout.body_measurement.UpdateBodyMeasurementUseCase
import com.vfdeginformatica.mysuperapp.presentation.screen.body_measurement_detail.contract.BodyMeasurementDetailEvent
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
class BodyMeasurementDetailViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private fun buildViewModel(
        measurementId: String?,
        repository: FakeBodyMeasurementRepository,
    ): BodyMeasurementDetailViewModel {
        val handle = SavedStateHandle(
            if (measurementId != null) {
                mapOf(BodyMeasurementDetailViewModel.MEASUREMENT_ID_ARG to measurementId)
            } else {
                emptyMap()
            }
        )
        return BodyMeasurementDetailViewModel(
            savedStateHandle = handle,
            getBodyMeasurementsUseCase = GetBodyMeasurementsUseCase(repository),
            addBodyMeasurementUseCase = AddBodyMeasurementUseCase(repository),
            updateBodyMeasurementUseCase = UpdateBodyMeasurementUseCase(repository),
            deleteBodyMeasurementUseCase = DeleteBodyMeasurementUseCase(repository),
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
    fun `blank weight disables submit with weight error`() = runTest {
        val vm = buildViewModel(null, FakeBodyMeasurementRepository())
        advanceUntilIdle()

        vm.onEvent(BodyMeasurementDetailEvent.HeightChanged("180"))
        vm.onEvent(BodyMeasurementDetailEvent.WeightChanged(""))

        val state = vm.uiState.value
        assertFalse(state.isSubmitEnabled)
        assertEquals("Informe o peso", state.weightError)
    }

    @Test
    fun `valid weight and height enable submit and save adds measurement`() = runTest {
        val repo = FakeBodyMeasurementRepository()
        val vm = buildViewModel(null, repo)
        advanceUntilIdle()

        vm.onEvent(BodyMeasurementDetailEvent.WeightChanged("72,5"))
        vm.onEvent(BodyMeasurementDetailEvent.HeightChanged("180"))

        val state = vm.uiState.value
        assertTrue(state.isSubmitEnabled)
        assertNull(state.weightError)
        assertNull(state.heightError)

        vm.onEvent(BodyMeasurementDetailEvent.SaveClicked)
        advanceUntilIdle()

        assertEquals(1, repo.added.size)
        assertEquals(72.5, repo.added.first().weightKg, 0.0001)
        assertEquals(180.0, repo.added.first().heightCm, 0.0001)
        assertTrue(repo.updated.isEmpty())
    }

    @Test
    fun `height below 50 produces height error`() = runTest {
        val vm = buildViewModel(null, FakeBodyMeasurementRepository())
        advanceUntilIdle()

        vm.onEvent(BodyMeasurementDetailEvent.WeightChanged("70"))
        vm.onEvent(BodyMeasurementDetailEvent.HeightChanged("40"))

        val state = vm.uiState.value
        assertEquals("Altura deve estar entre 50 e 300 cm", state.heightError)
        assertFalse(state.isSubmitEnabled)
    }

    @Test
    fun `future date produces date error`() = runTest {
        val vm = buildViewModel(null, FakeBodyMeasurementRepository())
        advanceUntilIdle()

        vm.onEvent(BodyMeasurementDetailEvent.WeightChanged("70"))
        vm.onEvent(BodyMeasurementDetailEvent.HeightChanged("180"))
        vm.onEvent(BodyMeasurementDetailEvent.DateChanged(LocalDate.now().plusDays(1)))

        val state = vm.uiState.value
        assertEquals("Data não pode ser futura", state.dateError)
        assertFalse(state.isSubmitEnabled)

        vm.onEvent(BodyMeasurementDetailEvent.DateChanged(LocalDate.now()))
        assertNull(vm.uiState.value.dateError)
        assertTrue(vm.uiState.value.isSubmitEnabled)
    }

    @Test
    fun `create mode prefills height from latest measurement`() = runTest {
        val repo = FakeBodyMeasurementRepository(
            measurements = listOf(
                BodyMeasurement("m1", 80.0, 175.5, LocalDate.of(2026, 10, 1)),
                BodyMeasurement("m0", 81.0, 170.0, LocalDate.of(2026, 9, 1)),
            )
        )
        val vm = buildViewModel(null, repo)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isEditing)
        assertEquals("175,5", state.heightText)
        assertEquals("", state.weightText)
    }

    @Test
    fun `edit mode loads existing record and save calls update`() = runTest {
        val repo = FakeBodyMeasurementRepository(
            measurements = listOf(
                BodyMeasurement("m1", 72.5, 180.0, LocalDate.of(2026, 10, 1), "manhã"),
            )
        )
        val vm = buildViewModel("m1", repo)
        advanceUntilIdle()

        val loaded = vm.uiState.value
        assertTrue(loaded.isEditing)
        assertEquals("72,5", loaded.weightText)
        assertEquals("180", loaded.heightText)
        assertEquals(LocalDate.of(2026, 10, 1), loaded.date)
        assertEquals("manhã", loaded.notes)
        assertTrue(loaded.isSubmitEnabled)

        vm.onEvent(BodyMeasurementDetailEvent.WeightChanged("73"))
        vm.onEvent(BodyMeasurementDetailEvent.SaveClicked)
        advanceUntilIdle()

        assertEquals(1, repo.updated.size)
        val saved = repo.updated.first()
        assertEquals("m1", saved.id)
        assertEquals(73.0, saved.weightKg, 0.0001)
        assertEquals(180.0, saved.heightCm, 0.0001)
        assertEquals("manhã", saved.notes)
        assertTrue(repo.added.isEmpty())
    }

    @Test
    fun `delete requires confirmation and then calls repository`() = runTest {
        val repo = FakeBodyMeasurementRepository(
            measurements = listOf(BodyMeasurement("m1", 72.5, 180.0, LocalDate.of(2026, 10, 1)))
        )
        val vm = buildViewModel("m1", repo)
        advanceUntilIdle()

        vm.onEvent(BodyMeasurementDetailEvent.DeleteClicked)
        assertTrue(vm.uiState.value.showDeleteConfirmation)

        vm.onEvent(BodyMeasurementDetailEvent.DeleteDismissed)
        assertFalse(vm.uiState.value.showDeleteConfirmation)
        assertTrue(repo.deletedIds.isEmpty())

        vm.onEvent(BodyMeasurementDetailEvent.DeleteClicked)
        vm.onEvent(BodyMeasurementDetailEvent.DeleteConfirmed)
        advanceUntilIdle()

        assertNotNull(repo.deletedIds.firstOrNull())
        assertEquals(listOf("m1"), repo.deletedIds)
    }
}
