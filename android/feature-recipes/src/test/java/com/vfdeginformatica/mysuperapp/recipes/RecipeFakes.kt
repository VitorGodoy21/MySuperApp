package com.vfdeginformatica.mysuperapp.recipes

import com.vfdeginformatica.mysuperapp.domain.model.Recipe
import com.vfdeginformatica.mysuperapp.domain.model.RecipeIngredient
import com.vfdeginformatica.mysuperapp.domain.repository.RecipeImageRepository
import com.vfdeginformatica.mysuperapp.domain.repository.RecipeRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow

fun validRecipe(id: String = "r1", owner: String = "alice") = Recipe(
    id = id, userId = owner, name = "Bolo",
    ingredients = listOf(RecipeIngredient("Farinha", "1 xícara")), steps = listOf("Misture"),
)

class FakeRecipes : RecipeRepository {
    val user = MutableStateFlow<String?>("alice")
    val recipes = mutableMapOf<String, Recipe>()
    val saved = mutableListOf<Recipe>()
    var failWrite = false
    var failRead = false
    var saveGate: CompletableDeferred<Unit>? = null
    var listGate: CompletableDeferred<Unit>? = null
    var afterSave: () -> Unit = {}

    override fun observeUserId() = user
    override fun currentUserId() = user.value
    override suspend fun list(userId: String): List<Recipe> {
        listGate?.await()
        check(!failRead) { "Falha na leitura" }
        return recipes.values.filter { it.userId == userId }
    }
    override suspend fun get(userId: String, id: String): Recipe? {
        check(!failRead) { "Falha na leitura" }
        return recipes[id]?.takeIf { it.userId == userId }
    }
    override suspend fun save(recipe: Recipe) {
        saveGate?.await()
        check(!failWrite) { "Falha ao salvar" }
        check(user.value == recipe.userId)
        saved += recipe
        recipes[recipe.id] = recipe
        afterSave()
    }
    override suspend fun delete(userId: String, id: String) {
        check(!failWrite)
        check(user.value == userId)
        recipes.remove(id)
    }
    override suspend fun favorite(userId: String, id: String, value: Boolean) {
        check(!failWrite)
        check(user.value == userId)
        recipes[id] = recipes.getValue(id).copy(isFavorite = value)
    }
}

class FakeRecipeImages : RecipeImageRepository {
    val deleted = mutableListOf<String>()
    val uploaded = mutableListOf<String>()
    var failUpload = false
    var failDelete = false
    var afterUpload: () -> Unit = {}
    override suspend fun upload(userId: String, recipeId: String, bytes: ByteArray): String {
        check(!failUpload) { "Upload falhou" }
        return "recipe_images/$userId/$recipeId/new.jpg".also { uploaded += it; afterUpload() }
    }
    override suspend fun read(userId: String, path: String) = byteArrayOf(1)
    override suspend fun delete(userId: String, path: String) {
        check(!failDelete) { "Limpeza falhou" }
        deleted += path
    }
}
