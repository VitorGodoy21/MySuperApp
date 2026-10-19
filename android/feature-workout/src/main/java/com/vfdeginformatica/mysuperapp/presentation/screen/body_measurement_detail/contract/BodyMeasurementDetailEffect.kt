package com.vfdeginformatica.mysuperapp.presentation.screen.body_measurement_detail.contract

sealed interface BodyMeasurementDetailEffect {
    data class ShowToast(val message: String) : BodyMeasurementDetailEffect
    data object NavigateUp : BodyMeasurementDetailEffect
}
