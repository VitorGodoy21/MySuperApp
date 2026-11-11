package com.vfdeginformatica.mysuperapp.domain.model

import java.text.Normalizer
import java.util.Locale

enum class RecipeCategory(val label: String) {
    STARTER("Entrada"), MAIN("Prato principal"), SIDE("Acompanhamento"),
    SNACK("Lanche"), DESSERT("Sobremesa"), DRINK("Bebida"),
}

enum class RecipeDifficulty(val label: String) {
    EASY("Fácil"), MEDIUM("Média"), HARD("Difícil"),
}

data class RecipeIngredient(val name: String = "", val quantity: String = "")

data class Recipe(
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val description: String = "",
    val ingredients: List<RecipeIngredient> = emptyList(),
    val steps: List<String> = emptyList(),
    val prepMinutes: Int? = null,
    val cookMinutes: Int? = null,
    val yield: String = "",
    val category: RecipeCategory? = null,
    val difficulty: RecipeDifficulty? = null,
    val notes: String = "",
    val isFavorite: Boolean = false,
    val imagePath: String? = null,
) {
    val totalMinutes: Int? get() =
        if (prepMinutes == null && cookMinutes == null) null else (prepMinutes ?: 0) + (cookMinutes ?: 0)

    fun normalized(): Recipe = copy(
        name = name.trim(), description = description.trim(), yield = yield.trim(), notes = notes.trim(),
        ingredients = ingredients.map { it.copy(name = it.name.trim(), quantity = it.quantity.trim()) },
        steps = steps.map(String::trim),
    )

    fun validationError(): String? = when {
        name.isBlank() || name.length > 200 -> "Informe um nome com até 200 caracteres"
        description.length > 4000 || notes.length > 4000 -> "Descrição e observações aceitam até 4000 caracteres"
        yield.length > 200 -> "O rendimento aceita até 200 caracteres"
        ingredients.isEmpty() || ingredients.size > 100 -> "Informe de 1 a 100 ingredientes"
        ingredients.any { it.name.isBlank() || it.quantity.isBlank() || it.name.length > 200 || it.quantity.length > 200 } ->
            "Preencha nome e quantidade de cada ingrediente (até 200 caracteres)"
        steps.isEmpty() || steps.size > 100 || steps.any { it.isBlank() || it.length > 4000 } ->
            "Informe de 1 a 100 passos, com até 4000 caracteres cada"
        listOfNotNull(prepMinutes, cookMinutes).any { it !in 0..999999 } -> "Informe tempos válidos em minutos"
        else -> null
    }
}

private fun String.recipeSearchKey(): String =
    Normalizer.normalize(this, Normalizer.Form.NFD).replace("\\p{M}+".toRegex(), "").lowercase(Locale.ROOT)

fun List<Recipe>.filterRecipes(query: String, favoritesOnly: Boolean): List<Recipe> {
    val key = query.trim().recipeSearchKey()
    return filter { (!favoritesOnly || it.isFavorite) && it.name.recipeSearchKey().contains(key) }
        .sortedBy { it.name.recipeSearchKey() }
}
