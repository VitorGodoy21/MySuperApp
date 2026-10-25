package com.vfdeginformatica.mysuperapp.data.remote.dto

import com.google.firebase.Timestamp

data class DietDto(
    val meals: List<DietMealDto> = emptyList(),
    val notes: String = "",
    val updatedAt: Timestamp? = null,
)

data class DietMealDto(
    val id: String = "",
    val name: String = "",
    val time: String = "",
    val notes: String = "",
    val foods: List<DietFoodDto> = emptyList(),
)

data class DietFoodDto(
    val id: String = "",
    val name: String = "",
    val quantity: String = "",
)

data class DietRecordDto(val ownerId: String, val diet: DietDto?)
