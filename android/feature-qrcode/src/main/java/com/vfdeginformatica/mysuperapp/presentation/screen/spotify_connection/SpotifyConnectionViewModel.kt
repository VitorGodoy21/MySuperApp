package com.vfdeginformatica.mysuperapp.presentation.screen.spotify_connection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.domain.model.SpotifyConnection
import com.vfdeginformatica.mysuperapp.domain.use_case.spotify.DisconnectSpotifyUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.spotify.ObserveSpotifyConnectionUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.spotify.StartSpotifyAuthUseCase
import com.vfdeginformatica.mysuperapp.presentation.screen.spotify_connection.contract.SpotifyConnectionEffect
import com.vfdeginformatica.mysuperapp.presentation.screen.spotify_connection.contract.SpotifyConnectionEvent
import com.vfdeginformatica.mysuperapp.presentation.screen.spotify_connection.contract.SpotifyConnectionUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SpotifyConnectionViewModel @Inject constructor(
    observeSpotifyConnectionUseCase: ObserveSpotifyConnectionUseCase,
    private val startSpotifyAuthUseCase: StartSpotifyAuthUseCase,
    private val disconnectSpotifyUseCase: DisconnectSpotifyUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SpotifyConnectionUiState())
    val uiState: StateFlow<SpotifyConnectionUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<SpotifyConnectionEffect>()
    val effect: SharedFlow<SpotifyConnectionEffect> = _effect.asSharedFlow()

    init {
        viewModelScope.launch {
            observeSpotifyConnectionUseCase().collect { connection ->
                _uiState.update {
                    it.copy(isLoading = false, connection = connection ?: SpotifyConnection())
                }
            }
        }
    }

    fun onEvent(event: SpotifyConnectionEvent) {
        when (event) {
            SpotifyConnectionEvent.OnConnectClick -> startAuth()
            SpotifyConnectionEvent.OnDisconnectClick -> _uiState.update { it.copy(showDisconnectConfirm = true) }
            SpotifyConnectionEvent.OnDisconnectDismiss -> _uiState.update { it.copy(showDisconnectConfirm = false) }
            SpotifyConnectionEvent.OnDisconnectConfirm -> {
                _uiState.update { it.copy(showDisconnectConfirm = false) }
                disconnect()
            }
        }
    }

    private fun startAuth() {
        if (_uiState.value.isBusy) return
        viewModelScope.launch {
            startSpotifyAuthUseCase().collect { resource ->
                when (resource) {
                    is Resource.Loading -> _uiState.update { it.copy(isStartingAuth = true, errorMessage = "") }
                    is Resource.Success -> {
                        _uiState.update { it.copy(isStartingAuth = false) }
                        resource.data?.let { _effect.emit(SpotifyConnectionEffect.OpenUrl(it)) }
                    }
                    is Resource.Error -> {
                        val message = resource.message ?: "Erro ao iniciar conexão"
                        _uiState.update { it.copy(isStartingAuth = false, errorMessage = message) }
                        _effect.emit(SpotifyConnectionEffect.ShowToast(message))
                    }
                }
            }
        }
    }

    private fun disconnect() {
        if (_uiState.value.isBusy) return
        viewModelScope.launch {
            disconnectSpotifyUseCase().collect { resource ->
                when (resource) {
                    is Resource.Loading -> _uiState.update { it.copy(isDisconnecting = true, errorMessage = "") }
                    is Resource.Success -> {
                        _uiState.update { it.copy(isDisconnecting = false) }
                        _effect.emit(SpotifyConnectionEffect.ShowToast("Spotify desconectado"))
                    }
                    is Resource.Error -> {
                        val message = resource.message ?: "Erro ao desconectar"
                        _uiState.update { it.copy(isDisconnecting = false, errorMessage = message) }
                        _effect.emit(SpotifyConnectionEffect.ShowToast(message))
                    }
                }
            }
        }
    }
}
