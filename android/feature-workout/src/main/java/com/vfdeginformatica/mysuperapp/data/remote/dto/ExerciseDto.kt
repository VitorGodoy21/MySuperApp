package com.vfdeginformatica.mysuperapp.data.remote.dto

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class ExerciseDto(
    @DocumentId
    var id: String = "",
    var name: String = "",
    var muscleGroup: String = "",
    var description: String = "",
    var notes: String = "",
    var videoUrl: String = "",
    var createdAt: Timestamp? = null,
)
