package com.vfdeginformatica.mysuperapp.data.remote.dto

import com.google.firebase.firestore.DocumentId
import java.util.Date

data class QrCodeAccessLogDto(
    @DocumentId var id: String = "",
    val qrCodeId: String = "",
    val timestamp: Date = Date(),
    val latitude: Double? = null,
    val longitude: Double? = null,
    val city: String = "",
    val country: String = "",
    val userAgent: String = ""
)

