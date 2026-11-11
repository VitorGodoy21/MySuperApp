package com.vfdeginformatica.mysuperapp.presentation.screen.qrcode.spotify

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.domain.model.SpotifyConnection
import com.vfdeginformatica.mysuperapp.domain.model.SpotifyDevice
import com.vfdeginformatica.mysuperapp.domain.model.SpotifyItem
import com.vfdeginformatica.mysuperapp.domain.model.SpotifyItemKind
import com.vfdeginformatica.mysuperapp.domain.model.SpotifyTarget
import com.vfdeginformatica.mysuperapp.domain.use_case.spotify.GetSpotifyDevicesUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.spotify.GetSpotifyPlaylistsUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.spotify.ObserveSpotifyConnectionUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.spotify.SearchSpotifyUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SpotifyPickerTab { MY_PLAYLISTS, SEARCH }

data class SpotifyPickerUiState(
    val connection: SpotifyConnection? = null,
    val isConnectionLoaded: Boolean = false,
    val tab: SpotifyPickerTab = SpotifyPickerTab.MY_PLAYLISTS,
    val playlists: List<SpotifyItem> = emptyList(),
    val isLoadingPlaylists: Boolean = false,
    val playlistsError: String = "",
    val query: String = "",
    val searchKind: SpotifyItemKind = SpotifyItemKind.PLAYLIST,
    val searchResults: List<SpotifyItem> = emptyList(),
    val isSearching: Boolean = false,
    val searchError: String = "",
    val devices: List<SpotifyDevice> = emptyList(),
    val isLoadingDevices: Boolean = false,
    val devicesError: String = "",
    val selectedItem: SpotifyItem? = null,
    val selectedDevice: SpotifyDevice? = null,
    val shuffle: Boolean = false
) {
    val isConnected: Boolean get() = connection?.isConnected == true && connection.needsReconnect.not()

    val canConfirm: Boolean get() = selectedItem != null && selectedDevice != null

    fun toTarget(): SpotifyTarget? {
        val item = selectedItem ?: return null
        val device = selectedDevice ?: return null
        return SpotifyTarget(
            uri = item.uri,
            name = item.name,
            subtitle = item.subtitle,
            imageUrl = item.imageUrl,
            deviceId = device.id,
            deviceName = device.name,
            shuffle = shuffle && item.kind != SpotifyItemKind.TRACK
        )
    }
}

@OptIn(FlowPreview::class)
@HiltViewModel
class SpotifyPickerViewModel @Inject constructor(
    observeSpotifyConnectionUseCase: ObserveSpotifyConnectionUseCase,
    private val getSpotifyPlaylistsUseCase: GetSpotifyPlaylistsUseCase,
    private val searchSpotifyUseCase: SearchSpotifyUseCase,
    private val getSpotifyDevicesUseCase: GetSpotifyDevicesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SpotifyPickerUiState())
    val uiState: StateFlow<SpotifyPickerUiState> = _uiState.asStateFlow()

    private val queryFlow = MutableStateFlow("")
    private var searchJob: Job? = null
    private var loadedForConnection = false

    init {
        viewModelScope.launch {
            observeSpotifyConnectionUseCase().collect { connection ->
                _uiState.update { it.copy(connection = connection, isConnectionLoaded = true) }
                if (_uiState.value.isConnected && !loadedForConnection) {
                    loadedForConnection = true
                    loadPlaylists()
                    loadDevices()
                }
            }
        }
        viewModelScope.launch {
            queryFlow.debounce(400).distinctUntilChanged().collect { runSearch(it) }
        }
    }

    /** Pré-seleciona o alvo atual ao editar uma tag existente. */
    fun initFrom(target: SpotifyTarget?) {
        if (target == null || _uiState.value.selectedItem != null) return
        _uiState.update {
            it.copy(
                selectedItem = SpotifyItem(
                    uri = target.uri,
                    kind = if (target.isTrack) SpotifyItemKind.TRACK else SpotifyItemKind.PLAYLIST,
                    name = target.name,
                    subtitle = target.subtitle,
                    imageUrl = target.imageUrl
                ),
                selectedDevice = SpotifyDevice(id = target.deviceId, name = target.deviceName),
                shuffle = target.shuffle
            )
        }
    }

    fun onTabChanged(tab: SpotifyPickerTab) = _uiState.update { it.copy(tab = tab) }

    fun onQueryChanged(query: String) {
        _uiState.update { it.copy(query = query) }
        queryFlow.value = query
    }

    fun onSearchKindChanged(kind: SpotifyItemKind) {
        _uiState.update { it.copy(searchKind = kind) }
        runSearch(_uiState.value.query)
    }

    fun onItemSelected(item: SpotifyItem) = _uiState.update { it.copy(selectedItem = item) }

    fun onDeviceSelected(device: SpotifyDevice) = _uiState.update { it.copy(selectedDevice = device) }

    fun onShuffleChanged(enabled: Boolean) = _uiState.update { it.copy(shuffle = enabled) }

    fun refreshDevices() = loadDevices()

    fun retryPlaylists() = loadPlaylists()

    private fun loadPlaylists() {
        viewModelScope.launch {
            getSpotifyPlaylistsUseCase().collect { resource ->
                when (resource) {
                    is Resource.Loading -> _uiState.update { it.copy(isLoadingPlaylists = true, playlistsError = "") }
                    is Resource.Success -> _uiState.update {
                        it.copy(isLoadingPlaylists = false, playlists = resource.data.orEmpty())
                    }
                    is Resource.Error -> _uiState.update {
                        it.copy(isLoadingPlaylists = false, playlistsError = resource.message ?: "Erro ao carregar playlists")
                    }
                }
            }
        }
    }

    private fun loadDevices() {
        viewModelScope.launch {
            getSpotifyDevicesUseCase().collect { resource ->
                when (resource) {
                    is Resource.Loading -> _uiState.update { it.copy(isLoadingDevices = true, devicesError = "") }
                    is Resource.Success -> _uiState.update { state ->
                        val devices = resource.data.orEmpty()
                        // Mantém a seleção vinda da tag se o dispositivo (id ou nome) continuar listado.
                        val selected = state.selectedDevice?.let { sel ->
                            devices.firstOrNull { it.id == sel.id }
                                ?: devices.firstOrNull { it.name.equals(sel.name, ignoreCase = true) }
                                ?: sel
                        }
                        state.copy(isLoadingDevices = false, devices = devices, selectedDevice = selected)
                    }
                    is Resource.Error -> _uiState.update {
                        it.copy(isLoadingDevices = false, devicesError = resource.message ?: "Erro ao listar dispositivos")
                    }
                }
            }
        }
    }

    private fun runSearch(query: String) {
        searchJob?.cancel()
        if (query.isBlank() || !_uiState.value.isConnected) {
            _uiState.update { it.copy(searchResults = emptyList(), isSearching = false, searchError = "") }
            return
        }
        searchJob = viewModelScope.launch {
            searchSpotifyUseCase(query, setOf(_uiState.value.searchKind)).collect { resource ->
                when (resource) {
                    is Resource.Loading -> _uiState.update { it.copy(isSearching = true, searchError = "") }
                    is Resource.Success -> _uiState.update {
                        it.copy(isSearching = false, searchResults = resource.data.orEmpty())
                    }
                    is Resource.Error -> _uiState.update {
                        it.copy(isSearching = false, searchError = resource.message ?: "Erro na busca")
                    }
                }
            }
        }
    }
}
