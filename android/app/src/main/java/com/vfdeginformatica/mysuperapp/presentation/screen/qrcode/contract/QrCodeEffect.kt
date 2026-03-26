package com.vfdeginformatica.mysuperapp.presentation.screen.qrcode.contract

sealed interface QrCodeEffect {
    data class ShowToast(val message: String) : QrCodeEffect
}
