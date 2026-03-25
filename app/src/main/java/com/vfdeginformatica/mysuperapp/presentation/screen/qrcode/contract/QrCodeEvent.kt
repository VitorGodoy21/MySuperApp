package com.vfdeginformatica.mysuperapp.presentation.screen.qrcode.contract

import com.vfdeginformatica.mysuperapp.domain.model.QrCode

sealed interface QrCodeEvent {
    data class OnQrCodeLoaded(val qrCode: QrCode) : QrCodeEvent
    data class OnRedirectUrlChanged(val url: String) : QrCodeEvent
    data object OnSaveQrCode : QrCodeEvent
}


