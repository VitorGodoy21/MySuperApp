package com.vfdeginformatica.mysuperapp.presentation.screen.qrcode.contract

import android.net.Uri
import com.vfdeginformatica.mysuperapp.domain.model.QrCode
import com.vfdeginformatica.mysuperapp.domain.model.QrCodeType

sealed interface QrCodeEvent {
    data class OnQrCodeLoaded(val qrCode: QrCode) : QrCodeEvent
    data class OnRedirectUrlChanged(val url: String) : QrCodeEvent
    data class OnTypeChanged(val type: QrCodeType) : QrCodeEvent
    data class OnTextChanged(val text: String) : QrCodeEvent
    data class OnImagePicked(val uri: Uri) : QrCodeEvent
    data object OnSaveQrCode : QrCodeEvent
}


