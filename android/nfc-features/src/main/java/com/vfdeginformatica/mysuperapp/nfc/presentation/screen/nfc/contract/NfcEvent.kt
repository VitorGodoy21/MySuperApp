package com.vfdeginformatica.mysuperapp.nfc.presentation.screen.nfc.contract

import android.nfc.Tag
import com.vfdeginformatica.mysuperapp.domain.model.QrCode

sealed interface NfcEvent {
    data object OnStartReading : NfcEvent
    data object OnStartWriting : NfcEvent
    data object OnCancel : NfcEvent
    data class OnSelectQrCode(val qrCode: QrCode) : NfcEvent
    data class OnTagDiscovered(val tag: Tag) : NfcEvent
    data object OnConfirmLock : NfcEvent
    data object OnDismissLockConfirmation : NfcEvent
}
