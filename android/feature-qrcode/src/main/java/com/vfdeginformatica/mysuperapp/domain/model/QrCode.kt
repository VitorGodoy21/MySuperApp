package com.vfdeginformatica.mysuperapp.domain.model

import android.graphics.Bitmap
import com.vfdeginformatica.mysuperapp.data.remote.dto.QrCodeDto

enum class QrCodeType {
    REDIRECT,
    TEXT,
    MURAL,
    IMAGE,
    SPOTIFY
}

/** Alvo do Spotify (playlist ou música) e dispositivo Spotify Connect de uma Smart Tag SPOTIFY. */
data class SpotifyTarget(
    val uri: String,
    val name: String,
    val subtitle: String = "",
    val imageUrl: String = "",
    val deviceId: String,
    val deviceName: String,
    val shuffle: Boolean = false
) {
    val isTrack: Boolean get() = uri.startsWith("spotify:track:")
}

data class QrCode(
    val id: String,
    val identifier: String = "",
    val redirectUrl: String,
    val staticUrl: String = "",
    val type: QrCodeType = QrCodeType.REDIRECT,
    val text: String = "",
    val userId: String = "",
    val imageUrl: String = "",
    val imagePath: String = "",
    val spotifyUri: String = "",
    val spotifyName: String = "",
    val spotifySubtitle: String = "",
    val spotifyImageUrl: String = "",
    val spotifyDeviceId: String = "",
    val spotifyDeviceName: String = "",
    val spotifyShuffle: Boolean = false,
    val qrcodeBitmap: Bitmap? = null
) {
    /** Alvo configurado, ou `null` quando a tag ainda não tem playlist/música e dispositivo. */
    val spotifyTarget: SpotifyTarget?
        get() = if (spotifyUri.isBlank() || spotifyDeviceId.isBlank() && spotifyDeviceName.isBlank()) null
        else SpotifyTarget(
            uri = spotifyUri,
            name = spotifyName,
            subtitle = spotifySubtitle,
            imageUrl = spotifyImageUrl,
            deviceId = spotifyDeviceId,
            deviceName = spotifyDeviceName,
            shuffle = spotifyShuffle
        )
}

fun QrCode.withSpotifyTarget(target: SpotifyTarget?): QrCode = copy(
    spotifyUri = target?.uri.orEmpty(),
    spotifyName = target?.name.orEmpty(),
    spotifySubtitle = target?.subtitle.orEmpty(),
    spotifyImageUrl = target?.imageUrl.orEmpty(),
    spotifyDeviceId = target?.deviceId.orEmpty(),
    spotifyDeviceName = target?.deviceName.orEmpty(),
    spotifyShuffle = target?.shuffle ?: false
)

fun QrCode.toDto(): QrCodeDto {
    return QrCodeDto(
        id = id,
        identifier = identifier,
        redirectUrl = redirectUrl,
        staticUrl = staticUrl,
        type = type.name,
        text = text,
        userId = userId,
        imageUrl = imageUrl,
        imagePath = imagePath,
        spotifyUri = spotifyUri,
        spotifyName = spotifyName,
        spotifySubtitle = spotifySubtitle,
        spotifyImageUrl = spotifyImageUrl,
        spotifyDeviceId = spotifyDeviceId,
        spotifyDeviceName = spotifyDeviceName,
        spotifyShuffle = spotifyShuffle
    )
}