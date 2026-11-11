package com.vfdeginformatica.mysuperapp.recipes

import androidx.lifecycle.SavedStateHandle
import com.vfdeginformatica.mysuperapp.domain.repository.RecipePhotoCompressor
import com.vfdeginformatica.mysuperapp.domain.use_case.recipes.*
import com.vfdeginformatica.mysuperapp.presentation.screen.recipe_detail.RecipeDetailViewModel
import com.vfdeginformatica.mysuperapp.presentation.screen.recipe_detail.contract.*
import com.vfdeginformatica.mysuperapp.presentation.screen.recipe_list.RecipeListViewModel
import com.vfdeginformatica.mysuperapp.presentation.screen.recipe_list.contract.*
import com.vfdeginformatica.mysuperapp.presentation.screen.recipe_view.RecipeViewViewModel
import com.vfdeginformatica.mysuperapp.presentation.screen.recipe_view.contract.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RecipeViewModelsTest {
    private val dispatcher = StandardTestDispatcher()
    private val repo = FakeRecipes()
    private val images = FakeRecipeImages()
    private val compressor = object : RecipePhotoCompressor {
        override suspend fun compress(uri: String) = byteArrayOf(1, 2)
    }
    private fun editor(id: String? = null) = RecipeDetailViewModel(
        SavedStateHandle(mapOf("recipeId" to id)), repo, GetRecipeUseCase(repo),
        SaveRecipeUseCase(repo, images), compressor, images,
    )
    private fun viewer() = RecipeViewViewModel(
        SavedStateHandle(mapOf("recipeId" to "r1")), repo, GetRecipeUseCase(repo),
        DeleteRecipeUseCase(repo, images), FavoriteRecipeUseCase(repo), images,
    )

    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun cleanup() { Dispatchers.resetMain() }

    @Test
    fun `list loads favorites and clears previous account data when account changes`() = runTest {
        repo.recipes["r1"] = validRecipe().copy(isFavorite = true)
        repo.recipes["b1"] = validRecipe("b1", "bob").copy(name = "Arroz")
        val vm = RecipeListViewModel(repo, GetRecipesUseCase(repo), images)
        advanceUntilIdle()
        assertEquals(listOf("r1"), vm.uiState.value.visibleRecipes.map { it.id })
        vm.onEvent(RecipeListEvent.QueryChanged("bolo"))
        vm.onEvent(RecipeListEvent.FavoritesToggled)
        assertEquals(1, vm.uiState.value.visibleRecipes.size)
        repo.user.value = "bob"
        advanceUntilIdle()
        assertEquals(listOf("b1"), vm.uiState.value.visibleRecipes.map { it.id })
        assertEquals("", vm.uiState.value.query)
        repo.user.value = null
        advanceUntilIdle()
        assertTrue(vm.uiState.value.recipes.isEmpty())
        assertNotNull(vm.uiState.value.error)
    }

    @Test
    fun `list can recover from a failed read`() = runTest {
        repo.failRead = true
        val vm = RecipeListViewModel(repo, GetRecipesUseCase(repo), images)
        advanceUntilIdle()
        assertNotNull(vm.uiState.value.error)
        repo.failRead = false
        repo.recipes["r1"] = validRecipe()
        vm.onEvent(RecipeListEvent.Reload)
        advanceUntilIdle()
        assertNull(vm.uiState.value.error)
        assertEquals(1, vm.uiState.value.recipes.size)
    }

    @Test
    fun `editor preserves draft after save failure and reuses id when retried`() = runTest {
        val vm = editor()
        advanceUntilIdle()
        val id = vm.uiState.value.draft.id
        vm.onEvent(RecipeDetailEvent.DraftChanged(validRecipe(id)))
        vm.onEvent(RecipeDetailEvent.PhotoSelected("content://photo"))
        repo.failWrite = true
        vm.onEvent(RecipeDetailEvent.Save)
        advanceUntilIdle()
        assertNotNull(vm.uiState.value.error)
        assertFalse(vm.uiState.value.isSaving)
        assertEquals("Bolo", vm.uiState.value.draft.name)
        assertEquals("content://photo", vm.uiState.value.photoUri)

        repo.failWrite = false
        vm.onEvent(RecipeDetailEvent.Save)
        vm.onEvent(RecipeDetailEvent.Save)
        advanceUntilIdle()
        assertEquals(listOf(id), repo.saved.map { it.id })
        assertFalse(vm.uiState.value.isDirty)
        assertNull(vm.uiState.value.photoUri)
    }

    @Test
    fun `editor asks before discarding and account change invalidates the draft`() = runTest {
        val vm = editor()
        advanceUntilIdle()
        vm.onEvent(RecipeDetailEvent.DraftChanged(vm.uiState.value.draft.copy(name = "Bolo")))
        vm.onEvent(RecipeDetailEvent.Back)
        assertTrue(vm.uiState.value.confirmLeave)
        vm.onEvent(RecipeDetailEvent.Stay)
        assertFalse(vm.uiState.value.confirmLeave)
        repo.user.value = "bob"
        advanceUntilIdle()
        assertTrue(vm.uiState.value.accountChanged)
        assertEquals("", vm.uiState.value.draft.name)
        vm.onEvent(RecipeDetailEvent.Save)
        advanceUntilIdle()
        assertTrue(repo.saved.isEmpty())
    }

    @Test
    fun `editor loads existing recipe for editing and cancels in-flight save on account change`() = runTest {
        repo.recipes["r1"] = validRecipe()
        val vm = editor("r1")
        advanceUntilIdle()
        assertEquals(validRecipe(), vm.uiState.value.draft)
        repo.saveGate = CompletableDeferred()
        vm.onEvent(RecipeDetailEvent.DraftChanged(vm.uiState.value.draft.copy(notes = "Mais sal")))
        vm.onEvent(RecipeDetailEvent.Save)
        runCurrent()
        assertTrue(vm.uiState.value.isSaving)
        repo.user.value = "bob"
        runCurrent()
        repo.saveGate!!.complete(Unit)
        advanceUntilIdle()
        assertTrue(vm.uiState.value.accountChanged)
        assertTrue(repo.saved.isEmpty())
    }

    @Test
    fun `viewer toggles favorite and only deletes after confirmation`() = runTest {
        repo.recipes["r1"] = validRecipe()
        val vm = viewer()
        advanceUntilIdle()
        vm.onEvent(RecipeViewEvent.ToggleFavorite)
        advanceUntilIdle()
        assertTrue(vm.uiState.value.recipe!!.isFavorite)
        assertTrue(repo.recipes.getValue("r1").isFavorite)
        vm.onEvent(RecipeViewEvent.ConfirmDelete)
        advanceUntilIdle()
        assertTrue(repo.recipes.containsKey("r1"))
        vm.onEvent(RecipeViewEvent.AskDelete)
        assertTrue(vm.uiState.value.confirmDelete)
        assertTrue(repo.recipes.containsKey("r1"))
        vm.onEvent(RecipeViewEvent.DismissDelete)
        assertFalse(vm.uiState.value.confirmDelete)
        vm.onEvent(RecipeViewEvent.AskDelete)
        vm.onEvent(RecipeViewEvent.ConfirmDelete)
        advanceUntilIdle()
        assertTrue(repo.recipes.isEmpty())
    }

    @Test
    fun `viewer clears details and rejects actions after logout`() = runTest {
        repo.recipes["r1"] = validRecipe()
        val vm = viewer()
        advanceUntilIdle()
        repo.user.value = null
        advanceUntilIdle()
        assertNull(vm.uiState.value.recipe)
        assertTrue(vm.uiState.value.accountChanged)
        vm.onEvent(RecipeViewEvent.ConfirmDelete)
        advanceUntilIdle()
        assertTrue(repo.recipes.containsKey("r1"))
    }
}
