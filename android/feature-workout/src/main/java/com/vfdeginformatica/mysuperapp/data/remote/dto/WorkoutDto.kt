package com.vfdeginformatica.mysuperapp.data.remote.dto

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

data class WorkoutItemDto(
    var exerciseId: String = "",
    var order: Int = 0,
    var sets: Int? = null,
    var reps: String? = null,
    var loadKg: Double? = null,
    var restSeconds: Int? = null,
    var notes: String = "",
)

data class WorkoutDto(
    @DocumentId
    var id: String = "",
    var name: String = "",
    var dayOfWeek: String? = null,
    var items: List<WorkoutItemDto> = emptyList(),
    /** Denormalized from [items] so Firestore `array-contains` can find workouts using an exercise. */
    var exerciseIds: List<String> = emptyList(),
    var notes: String = "",
    /** [com.vfdeginformatica.mysuperapp.domain.model.WorkoutIcon] name; blank on legacy documents. */
    var icon: String = "",
    /**
     * Legacy documents without the field read as `false`. The `is` prefix makes Firestore's bean
     * mapper name the property `other`, so the Firestore field name is pinned explicitly.
     */
    @get:PropertyName("isOther")
    @set:PropertyName("isOther")
    var isOther: Boolean = false,
    var createdAt: Timestamp? = null,
    var updatedAt: Timestamp? = null,
)
