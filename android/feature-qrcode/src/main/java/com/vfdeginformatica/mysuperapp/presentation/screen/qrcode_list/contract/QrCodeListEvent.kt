package com.vfdeginformatica.mysuperapp.presentation.screen.qrcode_list.contract

import com.vfdeginformatica.mysuperapp.domain.model.QrCode
import com.vfdeginformatica.mysuperapp.domain.model.QrCodeType

sealed interface QrCodeListEvent {
    data class OnSelectQrCode(val qrCode: QrCode) : QrCodeListEvent
    data class OnCreateQrCode(
        val type: QrCodeType,
        val identifier: String
    ) : QrCodeListEvent
}
