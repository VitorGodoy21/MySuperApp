package com.vfdeginformatica.mysuperapp.presentation.screen.body_measurement_list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.domain.use_case.workout.body_measurement.DeleteBodyMeasurementUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.workout.body_measurement.GetBodyMeasurementsUseCase
import com.vfdeginformatica.mysuperapp.presentation.screen.body_measurement_list.contract.BodyMeasurementListEffect
import com.vfdeginformatica.mysuperapp.presentation.screen.body_measurement_list.contract.BodyMeasurementListEvent
import com.vfdeginformatica.mysuperapp.presentation.screen.body_measurement_list.contract.BodyMeasurementListUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BodyMeasurementListViewModel @Inject constructor(
    private val getBodyMeasurementsUseCase: GetBodyMeasurementsUseCase,
    private val deleteBodyMeasurementUseCase: DeleteBodyMeasurementUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BodyMeasurementListUiState())
    val uiState: StateFlow<BodyMeasurementListUiState> = _uiState

    private val _effect = MutableSharedFlow<BodyMeasurementListEffect>()
    val effect: SharedFlow<BodyMeasurementListEffect> = _effect

    init {
        loadMeasurements()
    }

    fun onEvent(event: BodyMeasurementListEvent) {
        when (event) {
            BodyMeasurementListEvent.Reload -> loadMeasurements()

            BodyMeasurementListEvent.NewClicked -> viewModelScope.launch {
                _effect.emit(BodyMeasurementListEffect.NavigateToDetail(null))
            }

            is BodyMeasurementListEvent.MeasurementClicked -> viewModelScope.launch {
                _effect.emit(BodyMeasurementListEffect.NavigateToDetail(event.id))
            }

            is BodyMeasurementListEvent.DeleteClicked -> _uiState.update {
                it.copy(pendingDeleteId = event.id)
            }

            BodyMeasurementListEvent.DeleteDismissed -> _uiState.update {
                it.copy(pendingDeleteId = null)
            }

            BodyMeasurementListEvent.DeleteConfirmed -> {
                val id = _uiState.value.pendingDeleteId ?: return
                _uiState.update { it.copy(pendingDeleteId = null) }
                delete(id)
            }
        }
    }

    private fun loadMeasurements() {
        viewModelScope.launch {
            getBodyMeasurementsUseCase().collect { result ->
                when (result) {
                    is Resource.Loading -> _uiState.update { it.copy(isLoading = true) }

                    is Resource.Success -> _uiState.update {
                        it.copy(
                            isLoading = false,
                            measurements = result.data.orEmpty(),
                            errorMessage = null,
                        )
                    }

                    is Resource.Error -> {
                        _uiState.update {
                            it.copy(isLoading = false, errorMessage = result.message)
                        }
                        _effect.emit(
                            BodyMeasurementListEffect.ShowToast(
                                result.message ?: "Erro ao carregar dados físicos"
                            )
                        )
                    }
                }
            }
        }
    }

    private fun delete(id: String) {
        if (_uiState.value.isDeleting) return

        viewModelScope.launch {
            deleteBodyMeasurementUseCase(id).collect { result ->
                when (result) {
                    is Resource.Loading -> _uiState.update { it.copy(isDeleting = true) }

                    is Resource.Success -> {
                        _uiState.update { it.copy(isDeleting = false) }
                        _effect.emit(BodyMeasurementListEffect.ShowToast("Registro excluído"))
                        loadMeasurements()
                    }

                    is Resource.Error -> {
                        _uiState.update { it.copy(isDeleting = false) }
                        _effect.emit(
                            BodyMeasurementListEffect.ShowToast(
                                result.message ?: "Erro ao excluir registro"
                            )
                        )
                    }
                }
            }
        }
    }
}
