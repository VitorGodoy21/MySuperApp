package com.vfdeginformatica.mysuperapp.domain.model

import java.time.LocalDate
import kotlin.math.pow

data class BodyMeasurement(
    val id: String = "",
    val weightKg: Double,
    val heightCm: Double,
    val measuredAt: LocalDate,
    val notes: String = "",
) {
    /** Body mass index; null when height is not usable. */
    val bmi: Double?
        get() = if (heightCm > 0) weightKg / (heightCm / 100.0).pow(2) else null
}
