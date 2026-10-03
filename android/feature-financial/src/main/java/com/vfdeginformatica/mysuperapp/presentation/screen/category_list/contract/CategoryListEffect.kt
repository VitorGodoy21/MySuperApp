package com.vfdeginformatica.mysuperapp.presentation.screen.category_list.contract

sealed interface CategoryListEffect {
    data class NavigateToCategoryDetail(val categoryId: String) : CategoryListEffect
    data object NavigateToNewCategory : CategoryListEffect
    data class ShowToast(val message: String) : CategoryListEffect
}
