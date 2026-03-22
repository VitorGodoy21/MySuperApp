package com.vfdeginformatica.mysuperapp.presentation.screen.qrcode_list.contract

sealed interface QrCodeListEvent {
    data object OnRefresh : QrCodeListEvent
}
