package com.vfdeginformatica.mysuperapp.presentation.screen.spotify_connection.contract

import com.vfdeginformatica.mysuperapp.domain.model.SpotifyConnection

data class SpotifyConnectionUiState(
    val isLoading: Boolean = true,
    val connection: SpotifyConnection = SpotifyConnection(),
    val isStartingAuth: Boolean = false,
    val isDisconnecting: Boolean = false,
    val showDisconnectConfirm: Boolean = false,
    val errorMessage: String = ""
) {
    val isBusy: Boolean get() = isStartingAuth || isDisconnecting
}

sealed class SpotifyConnectionEvent {
    data object OnConnectClick : SpotifyConnectionEvent()
    data object OnDisconnectClick : SpotifyConnectionEvent()
    data object OnDisconnectConfirm : SpotifyConnectionEvent()
    data object OnDisconnectDismiss : SpotifyConnectionEvent()
}

sealed class SpotifyConnectionEffect {
    data class ShowToast(val message: String) : SpotifyConnectionEffect()
    data class OpenUrl(val url: String) : SpotifyConnectionEffect()
}
