package com.vfdeginformatica.mysuperapp.data.remote.dto

import com.google.firebase.firestore.DocumentId
import com.google.firebase.Timestamp

data class RecipeIngredientDto(
    val name: String = "",
    val quantity: String = "",
)

data class RecipeDto(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val description: String = "",
    val ingredients: List<RecipeIngredientDto> = emptyList(),
    val steps: List<String> = emptyList(),
    val prepMinutes: Int? = null,
    val cookMinutes: Int? = null,
    val yield: String = "",
    val category: String? = null,
    val difficulty: String? = null,
    val notes: String = "",
    @get:com.google.firebase.firestore.PropertyName("isFavorite")
    @set:com.google.firebase.firestore.PropertyName("isFavorite")
    var isFavorite: Boolean = false,
    val imagePath: String? = null,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
)
