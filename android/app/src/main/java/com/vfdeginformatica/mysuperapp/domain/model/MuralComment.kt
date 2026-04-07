package com.vfdeginformatica.mysuperapp.domain.model

import com.google.firebase.Timestamp
import com.vfdeginformatica.mysuperapp.data.remote.dto.MuralCommentDto

data class MuralComment(
    val id: String = "",
    val author: String = "",
    val message: String = "",
    val timestamp: Timestamp? = null
)

fun MuralComment.toDto(): MuralCommentDto {
    return MuralCommentDto(
        id = id,
        author = author,
        text = message,
        timestamp = timestamp
    )
}

