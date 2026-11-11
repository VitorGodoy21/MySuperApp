package com.vfdeginformatica.mysuperapp.data.mapper

import com.vfdeginformatica.mysuperapp.domain.model.SpotifyConnection
import com.vfdeginformatica.mysuperapp.domain.model.SpotifyDevice
import com.vfdeginformatica.mysuperapp.domain.model.SpotifyItem
import com.vfdeginformatica.mysuperapp.domain.model.SpotifyItemKind

/** `users/{uid}.spotify` → [SpotifyConnection]; ausente = nunca conectado. */
fun Map<*, *>?.toSpotifyConnection(): SpotifyConnection {
    if (this == null) return SpotifyConnection()
    return SpotifyConnection(
        isConnected = this["connected"] as? Boolean ?: false,
        needsReconnect = this["needsReconnect"] as? Boolean ?: false,
        displayName = this["displayName"] as? String ?: "",
        product = this["product"] as? String ?: ""
    )
}

fun List<*>?.toSpotifyItems(): List<SpotifyItem> =
    this.orEmpty().mapNotNull { raw ->
        val map = raw as? Map<*, *> ?: return@mapNotNull null
        val uri = map["uri"] as? String ?: return@mapNotNull null
        val kind = when ((map["kind"] as? String)?.uppercase()) {
            "TRACK" -> SpotifyItemKind.TRACK
            "PLAYLIST" -> SpotifyItemKind.PLAYLIST
            else -> return@mapNotNull null
        }
        SpotifyItem(
            uri = uri,
            kind = kind,
            name = map["name"] as? String ?: "",
            subtitle = map["subtitle"] as? String ?: "",
            imageUrl = map["imageUrl"] as? String ?: ""
        )
    }

fun List<*>?.toSpotifyDevices(): List<SpotifyDevice> =
    this.orEmpty().mapNotNull { raw ->
        val map = raw as? Map<*, *> ?: return@mapNotNull null
        val id = map["id"] as? String ?: return@mapNotNull null
        if (id.isBlank()) return@mapNotNull null
        SpotifyDevice(
            id = id,
            name = map["name"] as? String ?: "",
            type = map["type"] as? String ?: "",
            isActive = map["isActive"] as? Boolean ?: false,
            isRestricted = map["isRestricted"] as? Boolean ?: false
        )
    }
