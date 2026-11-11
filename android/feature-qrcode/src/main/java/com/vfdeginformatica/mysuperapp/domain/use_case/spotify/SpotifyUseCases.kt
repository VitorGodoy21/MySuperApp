package com.vfdeginformatica.mysuperapp.domain.use_case.spotify

import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.domain.model.SpotifyConnection
import com.vfdeginformatica.mysuperapp.domain.model.SpotifyDevice
import com.vfdeginformatica.mysuperapp.domain.model.SpotifyErrorCode
import com.vfdeginformatica.mysuperapp.domain.model.SpotifyException
import com.vfdeginformatica.mysuperapp.domain.model.SpotifyItem
import com.vfdeginformatica.mysuperapp.domain.model.SpotifyItemKind
import com.vfdeginformatica.mysuperapp.domain.repository.SpotifyRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class ObserveSpotifyConnectionUseCase @Inject constructor(
    private val repository: SpotifyRepository
) {
    operator fun invoke(): Flow<SpotifyConnection?> = repository.observeConnection()
}

class StartSpotifyAuthUseCase @Inject constructor(
    private val repository: SpotifyRepository
) {
    /** Emite a URL de autorização que o app deve abrir no navegador. */
    operator fun invoke(): Flow<Resource<String>> = spotifyResource { repository.startAuth() }
}

class DisconnectSpotifyUseCase @Inject constructor(
    private val repository: SpotifyRepository
) {
    operator fun invoke(): Flow<Resource<Unit>> = spotifyResource { repository.disconnect() }
}

class GetSpotifyPlaylistsUseCase @Inject constructor(
    private val repository: SpotifyRepository
) {
    operator fun invoke(offset: Int = 0): Flow<Resource<List<SpotifyItem>>> =
        spotifyResource { repository.getMyPlaylists(offset) }
}

class SearchSpotifyUseCase @Inject constructor(
    private val repository: SpotifyRepository
) {
    operator fun invoke(query: String, kinds: Set<SpotifyItemKind>): Flow<Resource<List<SpotifyItem>>> =
        spotifyResource { repository.search(query.trim(), kinds) }
}

class GetSpotifyDevicesUseCase @Inject constructor(
    private val repository: SpotifyRepository
) {
    operator fun invoke(): Flow<Resource<List<SpotifyDevice>>> = spotifyResource { repository.getDevices() }
}

/** Mensagem amigável para cada código de erro do backend. */
fun SpotifyException.friendlyMessage(): String = when (code) {
    SpotifyErrorCode.NOT_CONNECTED -> "Conecte sua conta do Spotify nas configurações"
    SpotifyErrorCode.NEEDS_RECONNECT -> "A conexão com o Spotify expirou; conecte novamente"
    SpotifyErrorCode.PREMIUM_REQUIRED -> "Controlar a reprodução exige Spotify Premium"
    SpotifyErrorCode.RATE_LIMITED -> "Muitas requisições ao Spotify; tente em instantes"
    SpotifyErrorCode.DEVICE_NOT_FOUND -> "Dispositivo não encontrado no Spotify"
    SpotifyErrorCode.UNKNOWN -> message ?: "Erro ao falar com o Spotify"
}

private fun <T> spotifyResource(block: suspend () -> T): Flow<Resource<T>> = flow {
    emit(Resource.Loading())
    try {
        emit(Resource.Success(block()))
    } catch (e: SpotifyException) {
        emit(Resource.Error(e.friendlyMessage()))
    } catch (e: Exception) {
        emit(Resource.Error(e.message ?: "Erro ao falar com o Spotify"))
    }
}
