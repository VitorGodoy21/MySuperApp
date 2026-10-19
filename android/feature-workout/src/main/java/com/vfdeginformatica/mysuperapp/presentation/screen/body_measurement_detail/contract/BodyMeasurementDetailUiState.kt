package com.vfdeginformatica.mysuperapp.presentation.screen.body_measurement_detail.contract

import java.time.LocalDate

data class BodyMeasurementDetailUiState(
    val measurementId: String? = null,
    val isLoading: Boolean = false,
    val weightText: String = "",
    val heightText: String = "",
    val date: LocalDate = LocalDate.now(),
    val notes: String = "",
    val isSaving: Boolean = false,
    val isDeleting: Boolean = false,
    val showDeleteConfirmation: Boolean = false,
    val showDatePicker: Boolean = false,
    val weightError: String? = null,
    val heightError: String? = null,
    val dateError: String? = null,
    val isSubmitEnabled: Boolean = false,
) {
    val isEditing: Boolean
        get() = !measurementId.isNullOrBlank()

    val isBusy: Boolean
        get() = isSaving || isDeleting
}
