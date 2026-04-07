package com.vfdeginformatica.mysuperapp.presentation.screen.mural_comments.contract

sealed interface MuralCommentsEffect {
    data class ShowToast(val message: String) : MuralCommentsEffect
}

