package com.vfdeginformatica.mysuperapp.domain.model

import android.graphics.Bitmap
import com.vfdeginformatica.mysuperapp.data.remote.dto.QrCodeDto

data class QrCode(
    val id: String,
    val redirectUrl: String,
    val staticUrl: String = "",
    val qrcodeBitmap: Bitmap? = null
)

fun QrCode.toDto(): QrCodeDto {
    return QrCodeDto(
        id = id,
        redirectUrl = redirectUrl,
        staticUrl = staticUrl
    )

}