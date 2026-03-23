package com.vfdeginformatica.mysuperapp.presentation.screen.qrcode.contract

sealed interface QrCodeEvent {
    data class OnQrCodeGenerated(val data: String) : QrCodeEvent
}
