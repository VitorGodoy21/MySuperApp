package com.vfdeginformatica.mysuperapp.data.remote.dto

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class WorkoutSessionDto(
    @DocumentId
    var id: String = "",
    var workoutId: String = "",
    var date: Timestamp? = null,
    var durationMinutes: Int? = null,
    var calories: Int? = null,
    var createdAt: Timestamp? = null,
)
