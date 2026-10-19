package com.vfdeginformatica.mysuperapp.presentation.screen.body_measurement_detail.contract

import java.time.LocalDate

sealed interface BodyMeasurementDetailEvent {
    data class WeightChanged(val text: String) : BodyMeasurementDetailEvent
    data class HeightChanged(val text: String) : BodyMeasurementDetailEvent
    data class DateChanged(val date: LocalDate) : BodyMeasurementDetailEvent
    data class DatePickerToggled(val visible: Boolean) : BodyMeasurementDetailEvent
    data class NotesChanged(val text: String) : BodyMeasurementDetailEvent
    data object SaveClicked : BodyMeasurementDetailEvent
    data object DeleteClicked : BodyMeasurementDetailEvent
    data object DeleteConfirmed : BodyMeasurementDetailEvent
    data object DeleteDismissed : BodyMeasurementDetailEvent
}
