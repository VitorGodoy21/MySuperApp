package com.vfdeginformatica.mysuperapp.data.mapper

import com.vfdeginformatica.mysuperapp.data.remote.dto.DietDto
import com.vfdeginformatica.mysuperapp.data.remote.dto.DietFoodDto
import com.vfdeginformatica.mysuperapp.data.remote.dto.DietMealDto
import com.vfdeginformatica.mysuperapp.domain.model.Diet
import com.vfdeginformatica.mysuperapp.domain.model.DietFood
import com.vfdeginformatica.mysuperapp.domain.model.DietMeal

fun DietDto.toModel(): Diet = Diet(
    meals = meals.map { meal ->
        DietMeal(
            id = meal.id,
            name = meal.name,
            time = meal.time,
            notes = meal.notes,
            foods = meal.foods.map { DietFood(it.id, it.name, it.quantity) },
        )
    },
    notes = notes,
)

fun Diet.toDto(): DietDto = DietDto(
    meals = meals.map { meal ->
        DietMealDto(
            id = meal.id,
            name = meal.name,
            time = meal.time,
            notes = meal.notes,
            foods = meal.foods.map { DietFoodDto(it.id, it.name, it.quantity) },
        )
    },
    notes = notes,
)
