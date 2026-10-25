package com.vfdeginformatica.mysuperapp.presentation.screen.diet_view.contract

import com.vfdeginformatica.mysuperapp.domain.model.Diet

data class DietViewUiState(
    val isLoading: Boolean = true,
    val diet: Diet? = null,
    val error: String? = null,
)

sealed interface DietViewEvent {
    data object Reload : DietViewEvent
}
