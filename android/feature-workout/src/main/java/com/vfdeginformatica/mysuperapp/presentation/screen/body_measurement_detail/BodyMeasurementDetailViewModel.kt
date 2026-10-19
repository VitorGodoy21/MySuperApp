package com.vfdeginformatica.mysuperapp.presentation.screen.body_measurement_detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.domain.model.BodyMeasurement
import com.vfdeginformatica.mysuperapp.domain.use_case.workout.body_measurement.AddBodyMeasurementUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.workout.body_measurement.DeleteBodyMeasurementUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.workout.body_measurement.GetBodyMeasurementsUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.workout.body_measurement.UpdateBodyMeasurementUseCase
import com.vfdeginformatica.mysuperapp.presentation.common.parseDecimal
import com.vfdeginformatica.mysuperapp.presentation.common.sanitizeDecimalInput
import com.vfdeginformatica.mysuperapp.presentation.screen.body_measurement_detail.contract.BodyMeasurementDetailEffect
import com.vfdeginformatica.mysuperapp.presentation.screen.body_measurement_detail.contract.BodyMeasurementDetailEvent
import com.vfdeginformatica.mysuperapp.presentation.screen.body_measurement_detail.contract.BodyMeasurementDetailUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class BodyMeasurementDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getBodyMeasurementsUseCase: GetBodyMeasurementsUseCase,
    private val addBodyMeasurementUseCase: AddBodyMeasurementUseCase,
    private val updateBodyMeasurementUseCase: UpdateBodyMeasurementUseCase,
    private val deleteBodyMeasurementUseCase: DeleteBodyMeasurementUseCase,
) : ViewModel() {

    companion object {
        const val MEASUREMENT_ID_ARG = "measurementId"
        const val MAX_NOTES_LENGTH = 200
        const val MAX_WEIGHT_KG = 500.0
        const val MIN_HEIGHT_CM = 50.0
        const val MAX_HEIGHT_CM = 300.0
    }

    private val measurementId: String? =
        savedStateHandle.get<String>(MEASUREMENT_ID_ARG)?.takeIf { it.isNotBlank() }

    private val _uiState =
        MutableStateFlow(BodyMeasurementDetailUiState(measurementId = measurementId))
    val uiState: StateFlow<BodyMeasurementDetailUiState> = _uiState

    private val _effect = MutableSharedFlow<BodyMeasurementDetailEffect>()
    val effect: SharedFlow<BodyMeasurementDetailEffect> = _effect

    init {
        loadInitialData()
    }

    fun onEvent(event: BodyMeasurementDetailEvent) {
        when (event) {
            is BodyMeasurementDetailEvent.WeightChanged -> updateForm {
                it.copy(weightText = sanitizeDecimalInput(event.text, maxDecimals = 2, maxIntDigits = 3))
            }

            is BodyMeasurementDetailEvent.HeightChanged -> updateForm {
                it.copy(heightText = sanitizeDecimalInput(event.text, maxDecimals = 1, maxIntDigits = 3))
            }

            is BodyMeasurementDetailEvent.DateChanged -> updateForm {
                it.copy(date = event.date, showDatePicker = false)
            }

            is BodyMeasurementDetailEvent.DatePickerToggled -> _uiState.update {
                it.copy(showDatePicker = event.visible)
            }

            is BodyMeasurementDetailEvent.NotesChanged -> updateForm {
                it.copy(notes = event.text.take(MAX_NOTES_LENGTH))
            }

            BodyMeasurementDetailEvent.SaveClicked -> save()

            BodyMeasurementDetailEvent.DeleteClicked -> _uiState.update {
                it.copy(showDeleteConfirmation = true)
            }

            BodyMeasurementDetailEvent.DeleteDismissed -> _uiState.update {
                it.copy(showDeleteConfirmation = false)
            }

            BodyMeasurementDetailEvent.DeleteConfirmed -> {
                _uiState.update { it.copy(showDeleteConfirmation = false) }
                delete()
            }
        }
    }

    /**
     * Edit mode: loads the record by id. Create mode: prefills height from the latest
     * measurement so the user only has to type the new weight.
     */
    private fun loadInitialData() {
        viewModelScope.launch {
            getBodyMeasurementsUseCase().collect { result ->
                when (result) {
                    is Resource.Loading -> _uiState.update { it.copy(isLoading = true) }

                    is Resource.Success -> {
                        val measurements = result.data.orEmpty()
                        if (measurementId == null) {
                            val latest = measurements.firstOrNull()
                            updateForm {
                                it.copy(
                                    isLoading = false,
                                    heightText = latest?.let { m -> formatHeightInput(m.heightCm) }
                                        ?: it.heightText,
                                )
                            }
                        } else {
                            val measurement = measurements.find { it.id == measurementId }
                            if (measurement == null) {
                                _uiState.update { it.copy(isLoading = false) }
                                _effect.emit(
                                    BodyMeasurementDetailEffect.ShowToast("Registro não encontrado")
                                )
                            } else {
                                updateForm {
                                    it.copy(
                                        isLoading = false,
                                        weightText = formatWeightInput(measurement.weightKg),
                                        heightText = formatHeightInput(measurement.heightCm),
                                        date = measurement.measuredAt,
                                        notes = measurement.notes,
                                    )
                                }
                            }
                        }
                    }

                    is Resource.Error -> {
                        _uiState.update { it.copy(isLoading = false) }
                        _effect.emit(
                            BodyMeasurementDetailEffect.ShowToast(
                                result.message ?: "Erro ao carregar dados físicos"
                            )
                        )
                    }
                }
            }
        }
    }

    private fun updateForm(
        transform: (BodyMeasurementDetailUiState) -> BodyMeasurementDetailUiState,
    ) {
        _uiState.update { old -> validate(transform(old)) }
    }

    private fun validate(state: BodyMeasurementDetailUiState): BodyMeasurementDetailUiState {
        val weight = parseDecimal(state.weightText)
        val weightError = when {
            state.weightText.isBlank() -> "Informe o peso"
            weight == null || weight <= 0.0 -> "Peso inválido"
            weight > MAX_WEIGHT_KG -> "Peso deve ser até 500 kg"
            else -> null
        }

        val height = parseDecimal(state.heightText)
        val heightError = when {
            state.heightText.isBlank() -> "Informe a altura"
            height == null || height < MIN_HEIGHT_CM || height > MAX_HEIGHT_CM ->
                "Altura deve estar entre 50 e 300 cm"

            else -> null
        }

        val dateError = if (state.date.isAfter(LocalDate.now())) "Data não pode ser futura" else null

        return state.copy(
            weightError = weightError,
            heightError = heightError,
            dateError = dateError,
            isSubmitEnabled = weightError == null && heightError == null && dateError == null,
        )
    }

    private fun save() {
        val state = _uiState.value
        if (!state.isSubmitEnabled || state.isBusy) return

        val weight = parseDecimal(state.weightText) ?: return
        val height = parseDecimal(state.heightText) ?: return

        val measurement = BodyMeasurement(
            id = state.measurementId.orEmpty(),
            weightKg = weight,
            heightCm = height,
            measuredAt = state.date,
            notes = state.notes.trim(),
        )

        viewModelScope.launch {
            val flow = if (state.isEditing) {
                updateBodyMeasurementUseCase(measurement)
            } else {
                addBodyMeasurementUseCase(measurement)
            }
            flow.collect { result ->
                when (result) {
                    is Resource.Loading -> _uiState.update { it.copy(isSaving = true) }

                    is Resource.Success -> {
                        _uiState.update { it.copy(isSaving = false) }
                        _effect.emit(BodyMeasurementDetailEffect.ShowToast("Pesagem salva!"))
                        _effect.emit(BodyMeasurementDetailEffect.NavigateUp)
                    }

                    is Resource.Error -> {
                        _uiState.update { it.copy(isSaving = false) }
                        _effect.emit(
                            BodyMeasurementDetailEffect.ShowToast(
                                result.message ?: "Erro ao salvar pesagem"
                            )
                        )
                    }
                }
            }
        }
    }

    private fun delete() {
        val id = _uiState.value.measurementId ?: return
        if (_uiState.value.isBusy) return

        viewModelScope.launch {
            deleteBodyMeasurementUseCase(id).collect { result ->
                when (result) {
                    is Resource.Loading -> _uiState.update { it.copy(isDeleting = true) }

                    is Resource.Success -> {
                        _uiState.update { it.copy(isDeleting = false) }
                        _effect.emit(BodyMeasurementDetailEffect.ShowToast("Registro excluído"))
                        _effect.emit(BodyMeasurementDetailEffect.NavigateUp)
                    }

                    is Resource.Error -> {
                        _uiState.update { it.copy(isDeleting = false) }
                        _effect.emit(
                            BodyMeasurementDetailEffect.ShowToast(
                                result.message ?: "Erro ao excluir registro"
                            )
                        )
                    }
                }
            }
        }
    }

    private fun formatWeightInput(value: Double): String = formatInput(value, maxDecimals = 2)

    private fun formatHeightInput(value: Double): String = formatInput(value, maxDecimals = 1)

    /** Renders a stored value as the user would type it ("72,5", "180"), without trailing zeros. */
    private fun formatInput(value: Double, maxDecimals: Int): String {
        val text = String.format(Locale.ROOT, "%.${maxDecimals}f", value)
            .trimEnd('0')
            .trimEnd('.')
            .replace('.', ',')
        return sanitizeDecimalInput(text, maxDecimals = maxDecimals, maxIntDigits = 3)
    }
}
