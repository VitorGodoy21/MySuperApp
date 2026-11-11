package com.vfdeginformatica.mysuperapp.data.remote.repository

import com.vfdeginformatica.mysuperapp.data.remote.datasource.SpotifyRemoteDao
import com.vfdeginformatica.mysuperapp.domain.model.SpotifyConnection
import com.vfdeginformatica.mysuperapp.domain.model.SpotifyDevice
import com.vfdeginformatica.mysuperapp.domain.model.SpotifyItem
import com.vfdeginformatica.mysuperapp.domain.model.SpotifyItemKind
import com.vfdeginformatica.mysuperapp.domain.repository.SpotifyRepository
import kotlinx.coroutines.flow.Flow

class SpotifyRepositoryImpl(
    private val dao: SpotifyRemoteDao
) : SpotifyRepository {
    override fun observeConnection(): Flow<SpotifyConnection?> = dao.observeConnection()
    override suspend fun startAuth(): String = dao.startAuth()
    override suspend fun disconnect() = dao.disconnect()
    override suspend fun getMyPlaylists(offset: Int): List<SpotifyItem> = dao.getMyPlaylists(offset)
    override suspend fun search(query: String, kinds: Set<SpotifyItemKind>): List<SpotifyItem> =
        dao.search(query, kinds)
    override suspend fun getDevices(): List<SpotifyDevice> = dao.getDevices()
}
