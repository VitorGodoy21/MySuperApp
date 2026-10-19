package com.vfdeginformatica.mysuperapp.presentation.screen.body_measurement_list.contract

sealed interface BodyMeasurementListEffect {
    /** Null id means "create a new measurement". */
    data class NavigateToDetail(val id: String?) : BodyMeasurementListEffect
    data class ShowToast(val message: String) : BodyMeasurementListEffect
}
