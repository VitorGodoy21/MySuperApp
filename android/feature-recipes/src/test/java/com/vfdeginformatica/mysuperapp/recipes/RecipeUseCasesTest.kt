package com.vfdeginformatica.mysuperapp.recipes

import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.domain.use_case.recipes.*
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class RecipeUseCasesTest {
    private val repo = FakeRecipes()
    private val images = FakeRecipeImages()
    private val save = SaveRecipeUseCase(repo, images)
    private val oldPath = "recipe_images/alice/r1/old.jpg"

    @Test
    fun `create trims fields before writing and reports validation without upload`() = runTest {
        val draft = validRecipe().copy(name = " Bolo ")
        val result = save(draft, RecipePhotoChange.Keep).toList().last()
        assertTrue(result is Resource.Success)
        assertEquals("Bolo", repo.saved.single().name)
        val invalid = save(draft.copy(steps = emptyList()), RecipePhotoChange.Replace(byteArrayOf(1))).toList().last()
        assertTrue(invalid is Resource.Error)
        assertTrue(images.uploaded.isEmpty())
        assertEquals(1, repo.saved.size)
    }

    @Test
    fun `replacement saves new photo before deleting old and removal clears its path`() = runTest {
        val recipe = validRecipe().copy(imagePath = oldPath)
        val result = save(recipe, RecipePhotoChange.Replace(byteArrayOf(1))).toList().last()
        assertTrue(result is Resource.Success)
        assertEquals(images.uploaded.single(), repo.saved.single().imagePath)
        assertEquals(listOf(oldPath), images.deleted)

        images.deleted.clear()
        save(recipe, RecipePhotoChange.Remove).toList()
        assertNull(repo.saved.last().imagePath)
        assertEquals(listOf(oldPath), images.deleted)
    }

    @Test
    fun `upload failure does not write or delete old image`() = runTest {
        images.failUpload = true
        val result = save(validRecipe().copy(imagePath = oldPath), RecipePhotoChange.Replace(byteArrayOf(1))).toList().last()
        assertTrue(result is Resource.Error)
        assertTrue(repo.saved.isEmpty())
        assertTrue(images.deleted.isEmpty())
    }

    @Test
    fun `failed save cleans only the newly uploaded image`() = runTest {
        repo.failWrite = true
        val result = save(validRecipe().copy(imagePath = oldPath), RecipePhotoChange.Replace(byteArrayOf(1))).toList().last()
        assertTrue(result is Resource.Error)
        assertEquals(images.uploaded, images.deleted)
        assertFalse(images.deleted.contains(oldPath))
    }

    @Test
    fun `cleanup failures do not undo a successful replacement or deletion`() = runTest {
        images.failDelete = true
        val recipe = validRecipe().copy(imagePath = oldPath)
        assertTrue(save(recipe, RecipePhotoChange.Remove).toList().last() is Resource.Success)
        repo.recipes[recipe.id] = recipe
        assertTrue(DeleteRecipeUseCase(repo, images)(recipe).toList().last() is Resource.Success)
        assertTrue(repo.recipes.isEmpty())
    }

    @Test
    fun `changing accounts during upload prevents writing the old draft`() = runTest {
        images.afterUpload = { repo.user.value = "bob" }
        val result = save(validRecipe(), RecipePhotoChange.Replace(byteArrayOf(1))).toList().last()
        assertTrue(result is Resource.Error)
        assertTrue(repo.saved.isEmpty())
    }

    @Test
    fun `cancellation after starting a write does not remove a possibly committed photo`() = runTest {
        repo.afterSave = { throw CancellationException("Await cancelled after server commit") }
        try {
            save(validRecipe(), RecipePhotoChange.Replace(byteArrayOf(1))).toList()
            fail("Cancellation must propagate")
        } catch (_: CancellationException) {
            assertEquals(images.uploaded.single(), repo.saved.single().imagePath)
            assertTrue(images.deleted.isEmpty())
        }
    }
}
