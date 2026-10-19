package com.vfdeginformatica.mysuperapp.data.remote.dto

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class BodyMeasurementDto(
    @DocumentId
    var id: String = "",
    var weightKg: Double = 0.0,
    var heightCm: Double = 0.0,
    var measuredAt: Timestamp? = null,
    var notes: String = "",
    var createdAt: Timestamp? = null,
)
