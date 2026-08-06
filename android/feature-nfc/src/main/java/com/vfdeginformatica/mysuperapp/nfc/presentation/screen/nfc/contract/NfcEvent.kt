package com.vfdeginformatica.mysuperapp.nfc.presentation.screen.nfc.contract

import android.nfc.Tag
import com.vfdeginformatica.mysuperapp.domain.model.QrCode

sealed interface NfcEvent {
    data object OnStartReading : NfcEvent
    data object OnStartWriting : NfcEvent
    data object OnCancel : NfcEvent
    data class OnSelectWriteSource(val source: NfcWriteSource) : NfcEvent
    data class OnSelectQrCode(val qrCode: QrCode) : NfcEvent
    data class OnCustomTextChanged(val text: String) : NfcEvent
    data object OnConfirmCustomText : NfcEvent
    data class OnTagDiscovered(val tag: Tag) : NfcEvent
    data object OnConfirmLock : NfcEvent
    data object OnDismissLockConfirmation : NfcEvent
    data object OnEditFromRead : NfcEvent
    data object OnLockPermanentlyFromRead : NfcEvent
}
