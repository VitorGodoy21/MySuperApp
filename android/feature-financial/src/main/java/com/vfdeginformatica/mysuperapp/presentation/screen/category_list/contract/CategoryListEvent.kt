package com.vfdeginformatica.mysuperapp.presentation.screen.category_list.contract

sealed interface CategoryListEvent {
    data object Reload : CategoryListEvent
    data class CategoryClicked(val categoryId: String) : CategoryListEvent
    data object NewCategoryClicked : CategoryListEvent
}
