package com.vfdeginformatica.mysuperapp.presentation.screen.access_log_map.contract

import com.vfdeginformatica.mysuperapp.domain.model.AccessLog
import com.vfdeginformatica.mysuperapp.domain.model.CityAccessStatistics

data class AccessLogMapUiState(
    val isLoading: Boolean = false,
    val accessLogs: List<AccessLog> = emptyList(),
    val cityStatistics: List<CityAccessStatistics> = emptyList(),
    val selectedCity: String? = null,
    val errorMessage: String = "",
    val viewMode: MapViewMode = MapViewMode.MAP,
    val selectedLog: AccessLog? = null,
    val isDeleting: Boolean = false
)

enum class MapViewMode {
    MAP,
    CITY_LIST,
    LOG_LIST
}

sealed interface AccessLogMapEvent {
    data class OnLoadAccessLogs(val qrCodeId: String) : AccessLogMapEvent
    data class OnSelectCity(val city: String) : AccessLogMapEvent
    data object OnClearCityFilter : AccessLogMapEvent
    data object OnToggleViewMode : AccessLogMapEvent
    data object OnShowLogList : AccessLogMapEvent
    data class OnSelectLog(val log: AccessLog) : AccessLogMapEvent
    data object OnClearSelectedLog : AccessLogMapEvent
    data class OnDeleteLog(val qrCodeId: String, val logId: String) : AccessLogMapEvent
}

sealed interface AccessLogMapEffect {
    data class ShowToast(val message: String) : AccessLogMapEffect
    data class ShowError(val error: String) : AccessLogMapEffect
}
