package com.vfdeginformatica.mysuperapp.domain.model

data class Diet(
    val meals: List<DietMeal> = emptyList(),
    val notes: String = "",
)

data class DietMeal(
    val id: String,
    val name: String,
    val time: String = "",
    val notes: String = "",
    val foods: List<DietFood> = emptyList(),
)

data class DietFood(
    val id: String,
    val name: String,
    val quantity: String,
)

data class DietRecord(val ownerId: String, val diet: Diet?)
