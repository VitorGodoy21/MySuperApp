package com.vfdeginformatica.mysuperapp.domain.model

data class Exercise(
    val id: String = "",
    val name: String,
    val muscleGroup: MuscleGroup? = null,
    val description: String = "",
    val notes: String = "",
)
