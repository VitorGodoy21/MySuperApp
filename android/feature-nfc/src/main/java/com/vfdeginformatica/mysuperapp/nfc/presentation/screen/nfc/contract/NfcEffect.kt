package com.vfdeginformatica.mysuperapp.nfc.presentation.screen.nfc.contract

sealed interface NfcEffect {
    data class ShowToast(val message: String) : NfcEffect
}
