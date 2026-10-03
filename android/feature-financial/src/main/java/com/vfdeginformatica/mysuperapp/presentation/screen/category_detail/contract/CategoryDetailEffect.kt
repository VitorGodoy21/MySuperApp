package com.vfdeginformatica.mysuperapp.presentation.screen.category_detail.contract

sealed interface CategoryDetailEffect {
    data class ShowToast(val message: String) : CategoryDetailEffect
    data object NavigateUp : CategoryDetailEffect
}
