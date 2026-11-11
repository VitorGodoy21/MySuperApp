package com.vfdeginformatica.mysuperapp.recipes

import com.vfdeginformatica.mysuperapp.data.mapper.toDto
import com.vfdeginformatica.mysuperapp.data.mapper.toModel
import com.vfdeginformatica.mysuperapp.domain.model.*
import com.vfdeginformatica.mysuperapp.presentation.screen.recipe_detail.moved
import org.junit.Assert.*
import org.junit.Test

class RecipeTest {
    @Test
    fun `search normalizes accents and combines favorites and alphabetical ordering`() {
        val recipes = listOf(
            validRecipe("b").copy(name = "Pão", isFavorite = true),
            validRecipe("a").copy(name = "Arroz", isFavorite = true),
            validRecipe("c").copy(name = "Pao doce"),
        )
        assertEquals(listOf("b", "c"), recipes.filterRecipes(" PÁO ", false).map { it.id })
        assertEquals(listOf("b"), recipes.filterRecipes("pao", true).map { it.id })
        assertEquals(listOf("a", "b"), recipes.filterRecipes("", true).map { it.id })
        assertTrue(recipes.filterRecipes("sopa", false).isEmpty())
    }

    @Test
    fun `requires complete recipe and limits invalid or oversized fields`() {
        val recipe = validRecipe()
        assertNull(recipe.validationError())
        listOf(
            recipe.copy(name = " "), recipe.copy(ingredients = emptyList()), recipe.copy(steps = listOf("")),
            recipe.copy(ingredients = listOf(RecipeIngredient("Farinha", ""))),
            recipe.copy(prepMinutes = -1), recipe.copy(cookMinutes = 1000000),
            recipe.copy(steps = listOf("x".repeat(4001))), recipe.copy(notes = "x".repeat(4001)),
        ).forEach { assertNotNull(it.validationError()) }
        assertNull(recipe.copy(prepMinutes = 0, cookMinutes = null).validationError())
    }

    @Test
    fun `mapper preserves optional metadata favorites photo and ordered entries`() {
        val recipe = validRecipe().copy(
            description = "Descrição", prepMinutes = 20, cookMinutes = 40, yield = "4 porções",
            category = RecipeCategory.DESSERT, difficulty = RecipeDifficulty.MEDIUM, isFavorite = true,
            imagePath = "recipe_images/alice/r1/photo.jpg",
            ingredients = listOf(RecipeIngredient("Ovos", "2"), RecipeIngredient("Sal", "1 pitada")),
            steps = listOf("Bata", "Asse"),
        )
        assertEquals(recipe, recipe.toDto().toModel())
        assertEquals(60, recipe.totalMinutes)
        assertNull(validRecipe().totalMinutes)
        assertEquals(listOf("Asse", "Bata"), recipe.steps.moved(0, 1))
    }
}
