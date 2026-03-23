package com.vfdeginformatica.mysuperapp.data.remote.dto

import com.google.firebase.firestore.DocumentId

data class QrCodeDto(
    @DocumentId var id: String = "",
    val redirectUrl: String = ""
)
