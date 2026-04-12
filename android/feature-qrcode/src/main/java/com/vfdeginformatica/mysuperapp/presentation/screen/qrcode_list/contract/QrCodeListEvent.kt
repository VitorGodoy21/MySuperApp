package com.vfdeginformatica.mysuperapp.presentation.screen.qrcode_list.contract

import com.vfdeginformatica.mysuperapp.domain.model.QrCode

sealed interface QrCodeListEvent {
    data class OnSelectQrCode(val qrCode: QrCode) : QrCodeListEvent
}
