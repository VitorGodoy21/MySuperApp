package com.vfdeginformatica.mysuperapp.presentation.screen.qrcode_list.contract

import com.vfdeginformatica.mysuperapp.domain.model.QrCode

sealed interface QrCodeListEffect {
    data class ShowToast(val message: String) : QrCodeListEffect
    data class NavigateToQrCode(val qrCode: QrCode) : QrCodeListEffect
}
