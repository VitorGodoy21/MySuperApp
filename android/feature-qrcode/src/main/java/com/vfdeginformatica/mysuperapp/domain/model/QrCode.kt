package com.vfdeginformatica.mysuperapp.domain.model

import android.graphics.Bitmap
import com.vfdeginformatica.mysuperapp.data.remote.dto.QrCodeDto

enum class QrCodeType {
    REDIRECT,
    TEXT,
    MURAL
}

data class QrCode(
    val id: String,
    val identifier: String = "",
    val redirectUrl: String,
    val staticUrl: String = "",
    val type: QrCodeType = QrCodeType.REDIRECT,
    val text: String = "",
    val userId: String = "",
    val qrcodeBitmap: Bitmap? = null
)

fun QrCode.toDto(): QrCodeDto {
    return QrCodeDto(
        id = id,
        identifier = identifier,
        redirectUrl = redirectUrl,
        staticUrl = staticUrl,
        type = type.name,
        text = text,
        userId = userId
    )

}