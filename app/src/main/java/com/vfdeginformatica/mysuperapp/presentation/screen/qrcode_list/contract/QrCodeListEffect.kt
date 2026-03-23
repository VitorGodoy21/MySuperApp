package com.vfdeginformatica.mysuperapp.presentation.screen.qrcode_list.contract

sealed interface QrCodeListEffect {
    data class ShowToast(val message: String) : QrCodeListEffect
    data class NavigateToQrCode(val id: String) : QrCodeListEffect
}
