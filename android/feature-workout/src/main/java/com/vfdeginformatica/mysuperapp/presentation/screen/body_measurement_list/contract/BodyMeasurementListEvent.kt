package com.vfdeginformatica.mysuperapp.presentation.screen.body_measurement_list.contract

sealed interface BodyMeasurementListEvent {
    data object Reload : BodyMeasurementListEvent
    data object NewClicked : BodyMeasurementListEvent
    data class MeasurementClicked(val id: String) : BodyMeasurementListEvent
    data class DeleteClicked(val id: String) : BodyMeasurementListEvent
    data object DeleteConfirmed : BodyMeasurementListEvent
    data object DeleteDismissed : BodyMeasurementListEvent
}
