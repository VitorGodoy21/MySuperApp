package com.vfdeginformatica.mysuperapp.presentation.screen.mural_comments.contract

sealed interface MuralCommentsEvent {
    data class OnLoadComments(val qrCodeId: String) : MuralCommentsEvent
    data class OnDeleteComment(val qrCodeId: String, val commentId: String) : MuralCommentsEvent
    data class OnCommentTextChanged(val text: String) : MuralCommentsEvent
    data class OnSendComment(val qrCodeId: String) : MuralCommentsEvent
}

