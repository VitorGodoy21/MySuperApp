package com.vfdeginformatica.mysuperapp.presentation.screen.access_log_map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vfdeginformatica.mysuperapp.domain.use_case.qrcode.DeleteAccessLogUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.qrcode.GetAccessLogsWithLocationsUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.qrcode.GetCityAccessStatisticsUseCase
import com.vfdeginformatica.mysuperapp.presentation.screen.access_log_map.contract.AccessLogMapEffect
import com.vfdeginformatica.mysuperapp.presentation.screen.access_log_map.contract.AccessLogMapEvent
import com.vfdeginformatica.mysuperapp.presentation.screen.access_log_map.contract.AccessLogMapUiState
import com.vfdeginformatica.mysuperapp.presentation.screen.access_log_map.contract.MapViewMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AccessLogMapViewModel @Inject constructor(
    private val getAccessLogsWithLocationsUseCase: GetAccessLogsWithLocationsUseCase,
    private val getCityAccessStatisticsUseCase: GetCityAccessStatisticsUseCase,
    private val deleteAccessLogUseCase: DeleteAccessLogUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccessLogMapUiState())
    val uiState: StateFlow<AccessLogMapUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<AccessLogMapEffect>()
    val effect: SharedFlow<AccessLogMapEffect> = _effect.asSharedFlow()

    // stored to reload after delete
    private var currentQrCodeId: String = ""

    fun onEvent(event: AccessLogMapEvent) {
        when (event) {
            is AccessLogMapEvent.OnLoadAccessLogs -> {
                currentQrCodeId = event.qrCodeId
                loadAccessLogs(event.qrCodeId)
            }

            is AccessLogMapEvent.OnSelectCity -> {
                _uiState.value = _uiState.value.copy(selectedCity = event.city)
            }

            is AccessLogMapEvent.OnClearCityFilter -> {
                _uiState.value = _uiState.value.copy(selectedCity = null)
            }

            is AccessLogMapEvent.OnToggleViewMode -> {
                val newMode = if (_uiState.value.viewMode == MapViewMode.MAP) {
                    MapViewMode.CITY_LIST
                } else {
                    MapViewMode.MAP
                }
                _uiState.value = _uiState.value.copy(viewMode = newMode)
            }

            is AccessLogMapEvent.OnShowLogList -> {
                _uiState.value = _uiState.value.copy(viewMode = MapViewMode.LOG_LIST)
            }

            is AccessLogMapEvent.OnSelectLog -> {
                _uiState.value = _uiState.value.copy(selectedLog = event.log)
            }

            is AccessLogMapEvent.OnClearSelectedLog -> {
                _uiState.value = _uiState.value.copy(selectedLog = null)
            }

            is AccessLogMapEvent.OnDeleteLog -> {
                deleteLog(event.qrCodeId, event.logId)
            }
        }
    }

    private fun deleteLog(qrCodeId: String, logId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDeleting = true)
            val success = deleteAccessLogUseCase(qrCodeId, logId)
            if (success) {
                // remove locally and reload stats
                val updatedLogs = _uiState.value.accessLogs.filter { it.id != logId }
                _uiState.value = _uiState.value.copy(
                    isDeleting = false,
                    accessLogs = updatedLogs,
                    selectedLog = null
                )
                sendEffect(AccessLogMapEffect.ShowToast("Log excluído com sucesso"))
            } else {
                _uiState.value = _uiState.value.copy(isDeleting = false)
                sendEffect(AccessLogMapEffect.ShowError("Erro ao excluir log"))
            }
        }
    }

    private fun loadAccessLogs(qrCodeId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = "")
            try {
                val accessLogs = getAccessLogsWithLocationsUseCase.invoke(qrCodeId)
                val cityStats = getCityAccessStatisticsUseCase.invoke(qrCodeId)

                if (accessLogs != null && cityStats != null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        accessLogs = accessLogs,
                        cityStatistics = cityStats
                    )
                    sendEffect(AccessLogMapEffect.ShowToast("Acessos carregados: ${accessLogs.size} registros"))
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Nenhum acesso encontrado"
                    )
                    sendEffect(AccessLogMapEffect.ShowError("Nenhum acesso encontrado"))
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Erro desconhecido"
                )
                sendEffect(AccessLogMapEffect.ShowError(e.message ?: "Erro ao carregar acessos"))
            }
        }
    }

    private fun sendEffect(effect: AccessLogMapEffect) {
        viewModelScope.launch {
            _effect.emit(effect)
        }
    }
}
