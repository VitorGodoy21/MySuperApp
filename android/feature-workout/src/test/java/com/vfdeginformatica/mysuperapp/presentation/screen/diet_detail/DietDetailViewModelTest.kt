package com.vfdeginformatica.mysuperapp.presentation.screen.diet_detail

import com.vfdeginformatica.mysuperapp.domain.model.Diet
import com.vfdeginformatica.mysuperapp.domain.model.DietMeal
import com.vfdeginformatica.mysuperapp.domain.model.DietFood
import com.vfdeginformatica.mysuperapp.domain.repository.FakeDietRepository
import com.vfdeginformatica.mysuperapp.domain.use_case.workout.diet.DeleteDietUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.workout.diet.GetDietUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.workout.diet.SaveDietUseCase
import com.vfdeginformatica.mysuperapp.presentation.screen.diet_detail.contract.DietDetailEvent
import com.vfdeginformatica.mysuperapp.presentation.screen.diet_detail.contract.DietDetailEffect
import kotlinx.coroutines.CompletableDeferred
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DietDetailViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private fun viewModel(repo: FakeDietRepository) =
        DietDetailViewModel(GetDietUseCase(repo), SaveDietUseCase(repo), DeleteDietUseCase(repo))

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `editing and reordering keep ids and stay local until save`() = runTest {
        val repo = FakeDietRepository()
        repo.diets["owner"] = Diet(listOf(
            DietMeal("a", "A", foods = listOf(DietFood("f1", "Arroz", "300 g"), DietFood("f2", "Ovos", "4"))),
            DietMeal("b", "B"),
        ))
        val vm = viewModel(repo)
        advanceUntilIdle()
        vm.onEvent(DietDetailEvent.MoveMeal("b", -1))
        vm.onEvent(DietDetailEvent.MoveFood("a", "f2", -1))
        vm.onEvent(DietDetailEvent.NotesChanged("Alterado"))
        assertEquals(listOf("b", "a"), vm.uiState.value.diet.meals.map { it.id })
        assertEquals(listOf("f2", "f1"), vm.uiState.value.diet.meals.last().foods.map { it.id })
        assertEquals(0, repo.writes)
        vm.onEvent(DietDetailEvent.Save)
        advanceUntilIdle()
        assertEquals(vm.uiState.value.diet, repo.diets["owner"])
        assertEquals(1, repo.writes)
    }

    @Test
    fun `adding editing and removing meals and foods only updates the draft`() = runTest {
        val repo = FakeDietRepository()
        val vm = viewModel(repo)
        advanceUntilIdle()
        vm.onEvent(DietDetailEvent.AddMeal)
        val meal = vm.uiState.value.diet.meals.single()
        vm.onEvent(DietDetailEvent.MealChanged(meal.copy(name = "Almoço", time = "12:00")))
        vm.onEvent(DietDetailEvent.AddFood(meal.id))
        val food = vm.uiState.value.diet.meals.single().foods.single()
        vm.onEvent(DietDetailEvent.FoodChanged(meal.id, food.copy(name = "Arroz", quantity = "300 g")))
        assertEquals("Almoço", vm.uiState.value.diet.meals.single().name)
        assertEquals("300 g", vm.uiState.value.diet.meals.single().foods.single().quantity)
        vm.onEvent(DietDetailEvent.RemoveFood(meal.id, food.id))
        assertTrue(vm.uiState.value.diet.meals.single().foods.isEmpty())
        vm.onEvent(DietDetailEvent.RemoveMeal(meal.id))
        assertTrue(vm.uiState.value.diet.meals.isEmpty())
        assertTrue(repo.diets.isEmpty())
    }

    @Test
    fun `failed loading cannot overwrite and failed saving preserves draft for retry`() = runTest {
        val repo = FakeDietRepository().apply { readError = "Sem conexão" }
        val vm = viewModel(repo)
        advanceUntilIdle()
        assertFalse(vm.uiState.value.canEdit)
        vm.onEvent(DietDetailEvent.Save)
        assertEquals(0, repo.writes)
        repo.readError = null
        vm.onEvent(DietDetailEvent.Retry)
        advanceUntilIdle()
        vm.onEvent(DietDetailEvent.NotesChanged("Rascunho"))
        repo.writeError = "Falha"
        vm.onEvent(DietDetailEvent.Save)
        advanceUntilIdle()
        assertEquals("Rascunho", vm.uiState.value.diet.notes)
        assertTrue(vm.uiState.value.canEdit)
        repo.writeError = null
        vm.onEvent(DietDetailEvent.Save)
        advanceUntilIdle()
        assertEquals("Rascunho", repo.diets["owner"]?.notes)
    }

    @Test
    fun `delete only runs after confirmation and cancelling preserves diet`() = runTest {
        val repo = FakeDietRepository().apply { diets["owner"] = Diet() }
        val vm = viewModel(repo)
        advanceUntilIdle()
        vm.onEvent(DietDetailEvent.DeleteConfirmed)
        assertEquals(0, repo.deletes)
        vm.onEvent(DietDetailEvent.DeleteClicked)
        vm.onEvent(DietDetailEvent.DeleteDismissed)
        assertFalse(vm.uiState.value.showDeleteConfirmation)
        assertNotNull(repo.diets["owner"])
        vm.onEvent(DietDetailEvent.DeleteClicked)
        vm.onEvent(DietDetailEvent.DeleteConfirmed)
        advanceUntilIdle()
        assertNull(repo.diets["owner"])
        assertEquals(1, repo.deletes)
    }

    @Test
    fun `account switch clears draft and ignores completion of previous account save`() = runTest {
        val repo = FakeDietRepository()
        val vm = viewModel(repo)
        val effects = mutableListOf<DietDetailEffect>()
        backgroundScope.launch { vm.effect.collect { effects += it } }
        advanceUntilIdle()
        vm.onEvent(DietDetailEvent.NotesChanged("Privado"))
        repo.saveGate = CompletableDeferred()
        vm.onEvent(DietDetailEvent.Save)
        advanceUntilIdle()
        repo.owner.value = "other"
        advanceUntilIdle()
        assertEquals("other", vm.uiState.value.ownerId)
        assertEquals(Diet(), vm.uiState.value.diet)
        repo.saveGate?.complete(Unit)
        advanceUntilIdle()
        assertTrue(effects.isEmpty())
        assertTrue(repo.diets.isEmpty())
        repo.owner.value = null
        advanceUntilIdle()
        assertNull(vm.uiState.value.ownerId)
        assertFalse(vm.uiState.value.canEdit)
    }
}
