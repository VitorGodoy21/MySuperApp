package com.vfdeginformatica.mysuperapp.data.remote.dto

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class MuralCommentDto(
    @DocumentId var id: String = "",
    var author: String = "",
    var text: String = "",
    var timestamp: Timestamp? = null,
    var highlighted: Boolean = true
)
