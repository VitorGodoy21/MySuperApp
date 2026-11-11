package com.vfdeginformatica.mysuperapp.domain.repository

import com.vfdeginformatica.mysuperapp.domain.model.SpotifyConnection
import com.vfdeginformatica.mysuperapp.domain.model.SpotifyDevice
import com.vfdeginformatica.mysuperapp.domain.model.SpotifyItem
import com.vfdeginformatica.mysuperapp.domain.model.SpotifyItemKind
import kotlinx.coroutines.flow.Flow

interface SpotifyRepository {
    fun observeConnection(): Flow<SpotifyConnection?>
    suspend fun startAuth(): String
    suspend fun disconnect()
    suspend fun getMyPlaylists(offset: Int = 0): List<SpotifyItem>
    suspend fun search(query: String, kinds: Set<SpotifyItemKind>): List<SpotifyItem>
    suspend fun getDevices(): List<SpotifyDevice>
}
