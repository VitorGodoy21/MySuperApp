package com.vfdeginformatica.mysuperapp.domain.model

/** Estado da conta Spotify vinculada (`users/{uid}.spotify`, escrito só pelo backend). */
data class SpotifyConnection(
    val isConnected: Boolean = false,
    val needsReconnect: Boolean = false,
    val displayName: String = "",
    /** `premium`, `free`, `open`… Controlar a reprodução exige `premium`. */
    val product: String = ""
) {
    val isPremium: Boolean get() = product.equals("premium", ignoreCase = true)
}

enum class SpotifyItemKind { PLAYLIST, TRACK }

/** Playlist ou música retornada pela busca/biblioteca do Spotify. */
data class SpotifyItem(
    val uri: String,
    val kind: SpotifyItemKind,
    val name: String,
    val subtitle: String = "",
    val imageUrl: String = ""
)

/** Dispositivo Spotify Connect (Alexa, grupo multi-room, celular…). */
data class SpotifyDevice(
    val id: String,
    val name: String,
    val type: String = "",
    val isActive: Boolean = false,
    val isRestricted: Boolean = false
) {
    /** Alexas e grupos aparecem como `Speaker`/`Unknown`; usado só para destacar na lista. */
    val isSpeaker: Boolean get() = type.equals("Speaker", ignoreCase = true) || type.equals("AVR", ignoreCase = true)
}

/** Erros da integração Spotify mapeados a partir dos códigos do backend. */
enum class SpotifyErrorCode {
    NOT_CONNECTED,
    NEEDS_RECONNECT,
    PREMIUM_REQUIRED,
    RATE_LIMITED,
    DEVICE_NOT_FOUND,
    UNKNOWN;

    companion object {
        fun from(raw: String?): SpotifyErrorCode =
            entries.firstOrNull { it.name == raw } ?: UNKNOWN
    }
}

class SpotifyException(val code: SpotifyErrorCode, message: String) : Exception(message)
