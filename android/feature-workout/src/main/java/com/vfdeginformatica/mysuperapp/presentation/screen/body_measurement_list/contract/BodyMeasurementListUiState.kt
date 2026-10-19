package com.vfdeginformatica.mysuperapp.presentation.screen.body_measurement_list.contract

import com.vfdeginformatica.mysuperapp.domain.model.BodyMeasurement
import com.vfdeginformatica.mysuperapp.presentation.common.WeightPoint

data class MeasurementRow(
    val measurement: BodyMeasurement,
    /** Weight difference against the previous (older) record; null for the oldest one. */
    val deltaKg: Double?,
)

data class BodyMeasurementListUiState(
    val isLoading: Boolean = false,
    /** Newest first. */
    val measurements: List<BodyMeasurement> = emptyList(),
    val errorMessage: String? = null,
    val pendingDeleteId: String? = null,
    val isDeleting: Boolean = false,
) {
    val latest: BodyMeasurement?
        get() = measurements.firstOrNull()

    /** Chronological points for the chart. */
    val chartPoints: List<WeightPoint>
        get() = measurements
            .sortedBy { it.measuredAt }
            .map { WeightPoint(date = it.measuredAt, weightKg = it.weightKg) }

    val rows: List<MeasurementRow>
        get() = measurements.mapIndexed { index, measurement ->
            val older = measurements.getOrNull(index + 1)
            MeasurementRow(
                measurement = measurement,
                deltaKg = older?.let { measurement.weightKg - it.weightKg },
            )
        }
}
