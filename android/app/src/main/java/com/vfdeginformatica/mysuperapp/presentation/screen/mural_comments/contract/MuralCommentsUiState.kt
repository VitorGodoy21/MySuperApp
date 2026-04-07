package com.vfdeginformatica.mysuperapp.presentation.screen.mural_comments.contract

import com.vfdeginformatica.mysuperapp.domain.model.MuralComment

data class MuralCommentsUiState(
    val isLoading: Boolean = false,
    val comments: List<MuralComment> = emptyList(),
    val errorMessage: String = ""
)

