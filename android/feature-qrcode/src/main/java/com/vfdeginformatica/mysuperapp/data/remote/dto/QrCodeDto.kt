package com.vfdeginformatica.mysuperapp.data.remote.dto

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class QrCodeDto(
    @DocumentId var id: String = "",
    var identifier: String = "",
    var redirectUrl: String = "",
    var staticUrl: String = "",
    var type: String = "REDIRECT",
    var text: String = "",
    var userId: String = ""
)
