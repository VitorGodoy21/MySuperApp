package com.vfdeginformatica.mysuperapp.data.mapper

import com.vfdeginformatica.mysuperapp.data.remote.dto.RecipeDto
import com.vfdeginformatica.mysuperapp.data.remote.dto.RecipeIngredientDto
import com.vfdeginformatica.mysuperapp.domain.model.Recipe
import com.vfdeginformatica.mysuperapp.domain.model.RecipeCategory
import com.vfdeginformatica.mysuperapp.domain.model.RecipeDifficulty
import com.vfdeginformatica.mysuperapp.domain.model.RecipeIngredient

fun RecipeDto.toModel() = Recipe(
    id, userId, name, description, ingredients.map { RecipeIngredient(it.name, it.quantity) },
    steps, prepMinutes, cookMinutes, yield,
    RecipeCategory.entries.find { it.name == category },
    RecipeDifficulty.entries.find { it.name == difficulty }, notes, isFavorite, imagePath,
)

fun Recipe.toDto() = RecipeDto(
    id = id, userId = userId, name = name, description = description,
    ingredients = ingredients.map { RecipeIngredientDto(it.name, it.quantity) },
    steps = steps, prepMinutes = prepMinutes, cookMinutes = cookMinutes, yield = yield,
    category = category?.name, difficulty = difficulty?.name, notes = notes,
    isFavorite = isFavorite, imagePath = imagePath,
)
