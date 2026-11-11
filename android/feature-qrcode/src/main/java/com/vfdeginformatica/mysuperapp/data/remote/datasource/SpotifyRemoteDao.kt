package com.vfdeginformatica.mysuperapp.data.remote.datasource

import com.vfdeginformatica.mysuperapp.domain.model.SpotifyConnection
import com.vfdeginformatica.mysuperapp.domain.model.SpotifyDevice
import com.vfdeginformatica.mysuperapp.domain.model.SpotifyItem
import com.vfdeginformatica.mysuperapp.domain.model.SpotifyItemKind
import kotlinx.coroutines.flow.Flow

/**
 * Acesso à integração Spotify: estado da conexão em `users/{uid}.spotify` e
 * callables do backend (`spotifyAuthStart`, `spotifyMyPlaylists`, …). Os
 * tokens OAuth nunca chegam ao app. Toda falha é lançada como
 * [com.vfdeginformatica.mysuperapp.domain.model.SpotifyException].
 */
interface SpotifyRemoteDao {
    /** Emite o estado atual e cada alteração; `null` quando deslogado. */
    fun observeConnection(): Flow<SpotifyConnection?>

    suspend fun startAuth(): String

    suspend fun disconnect()

    suspend fun getMyPlaylists(offset: Int = 0, limit: Int = 50): List<SpotifyItem>

    suspend fun search(query: String, kinds: Set<SpotifyItemKind>): List<SpotifyItem>

    suspend fun getDevices(): List<SpotifyDevice>
}
