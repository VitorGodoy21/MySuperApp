package com.vfdeginformatica.mysuperapp.presentation.screen.diet_detail.contract

import com.vfdeginformatica.mysuperapp.domain.model.Diet
import com.vfdeginformatica.mysuperapp.domain.model.DietFood
import com.vfdeginformatica.mysuperapp.domain.model.DietMeal

data class DietDetailUiState(
    val isLoading: Boolean = true,
    val ownerId: String? = null,
    val diet: Diet = Diet(),
    val exists: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val showDeleteConfirmation: Boolean = false,
) {
    val canEdit: Boolean get() = !isLoading && !isSaving && ownerId != null && error == null
}

sealed interface DietDetailEvent {
    data object Retry : DietDetailEvent
    data class NotesChanged(val notes: String) : DietDetailEvent
    data object AddMeal : DietDetailEvent
    data class MealChanged(val meal: DietMeal) : DietDetailEvent
    data class RemoveMeal(val id: String) : DietDetailEvent
    data class MoveMeal(val id: String, val offset: Int) : DietDetailEvent
    data class AddFood(val mealId: String) : DietDetailEvent
    data class FoodChanged(val mealId: String, val food: DietFood) : DietDetailEvent
    data class RemoveFood(val mealId: String, val foodId: String) : DietDetailEvent
    data class MoveFood(val mealId: String, val foodId: String, val offset: Int) : DietDetailEvent
    data object Save : DietDetailEvent
    data object DeleteClicked : DietDetailEvent
    data object DeleteDismissed : DietDetailEvent
    data object DeleteConfirmed : DietDetailEvent
}

sealed interface DietDetailEffect {
    data class ShowMessage(val message: String) : DietDetailEffect
    data object NavigateUp : DietDetailEffect
}
