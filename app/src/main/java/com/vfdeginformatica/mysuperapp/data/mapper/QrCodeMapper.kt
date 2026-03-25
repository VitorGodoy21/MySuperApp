package com.vfdeginformatica.mysuperapp.data.mapper

import com.vfdeginformatica.mysuperapp.data.remote.dto.QrCodeDto
import com.vfdeginformatica.mysuperapp.domain.model.QrCode
import com.vfdeginformatica.mysuperapp.domain.util.QrCodeGenerator

fun QrCodeDto.toModel(qrCodeGenerator: QrCodeGenerator): QrCode {
    return QrCode(
        id = this.id,
        redirectUrl = this.redirectUrl,
        staticUrl = this.staticUrl,
        qrcodeBitmap = qrCodeGenerator.generate(this.staticUrl)
    )
}
