package com.vfdeginformatica.mysuperapp.presentation.screen.diet_view

import com.vfdeginformatica.mysuperapp.domain.model.Diet
import com.vfdeginformatica.mysuperapp.domain.repository.FakeDietRepository
import com.vfdeginformatica.mysuperapp.domain.use_case.workout.diet.GetDietUseCase
import com.vfdeginformatica.mysuperapp.presentation.screen.diet_view.contract.DietViewEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DietViewViewModelTest {
    @Before fun setUp() { Dispatchers.setMain(StandardTestDispatcher()) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `consultation reloads changes and never writes and clears on account switch`() = runTest {
        val repo = FakeDietRepository().apply { diets["owner"] = Diet(notes = "Inicial") }
        val vm = DietViewViewModel(GetDietUseCase(repo))
        vm.onEvent(DietViewEvent.Reload)
        advanceUntilIdle()
        assertEquals("Inicial", vm.uiState.value.diet?.notes)
        repo.diets["owner"] = Diet(notes = "Atualizada")
        vm.onEvent(DietViewEvent.Reload)
        advanceUntilIdle()
        assertEquals("Atualizada", vm.uiState.value.diet?.notes)
        repo.owner.value = "other"
        advanceUntilIdle()
        assertNull(vm.uiState.value.diet)
        assertNull(vm.uiState.value.error)
        repo.readError = "Erro real"
        vm.onEvent(DietViewEvent.Reload)
        advanceUntilIdle()
        assertEquals("Erro real", vm.uiState.value.error)
        assertEquals(0, repo.writes)
        assertEquals(0, repo.deletes)
    }
}
