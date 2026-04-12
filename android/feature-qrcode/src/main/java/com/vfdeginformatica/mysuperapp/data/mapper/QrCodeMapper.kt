package com.vfdeginformatica.mysuperapp.data.mapper

import com.vfdeginformatica.mysuperapp.data.remote.dto.MuralCommentDto
import com.vfdeginformatica.mysuperapp.data.remote.dto.QrCodeDto
import com.vfdeginformatica.mysuperapp.domain.model.MuralComment
import com.vfdeginformatica.mysuperapp.domain.model.QrCode
import com.vfdeginformatica.mysuperapp.domain.model.QrCodeType
import com.vfdeginformatica.mysuperapp.domain.util.QrCodeGenerator

fun MuralCommentDto.toModel(): MuralComment {
    return MuralComment(
        id = this.id,
        author = this.author,
        message = this.text,
        timestamp = this.timestamp,
        highlighted = this.highlighted
    )
}

fun QrCodeDto.toModel(qrCodeGenerator: QrCodeGenerator): QrCode {
    return QrCode(
        id = this.id,
        identifier = this.identifier,
        redirectUrl = this.redirectUrl,
        staticUrl = this.staticUrl,
        type = runCatching { QrCodeType.valueOf(this.type) }.getOrDefault(QrCodeType.REDIRECT),
        text = this.text,
        userId = this.userId,
        qrcodeBitmap = qrCodeGenerator.generate(this.staticUrl)
    )
}
