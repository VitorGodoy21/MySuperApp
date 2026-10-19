package com.vfdeginformatica.mysuperapp.data.mapper

import com.google.firebase.Timestamp
import com.vfdeginformatica.mysuperapp.data.remote.dto.BodyMeasurementDto
import com.vfdeginformatica.mysuperapp.domain.model.BodyMeasurement
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

internal fun Timestamp.toLocalDate(): LocalDate =
    Instant.ofEpochSecond(seconds, nanoseconds.toLong())
        .atZone(ZoneId.systemDefault())
        .toLocalDate()

internal fun LocalDate.toTimestamp(): Timestamp =
    Timestamp(Date.from(atStartOfDay(ZoneId.systemDefault()).toInstant()))

fun BodyMeasurementDto.toModel(): BodyMeasurement = BodyMeasurement(
    id = id,
    weightKg = weightKg,
    heightCm = heightCm,
    measuredAt = measuredAt?.toLocalDate() ?: createdAt?.toLocalDate() ?: LocalDate.now(),
    notes = notes,
)

fun BodyMeasurement.toDto(): BodyMeasurementDto = BodyMeasurementDto(
    id = id,
    weightKg = weightKg,
    heightCm = heightCm,
    measuredAt = measuredAt.toTimestamp(),
    notes = notes,
)
