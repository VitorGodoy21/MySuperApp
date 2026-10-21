package com.vfdeginformatica.mysuperapp.notification

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Guarda o `qrCodeId` recebido pelo intent de uma notificação até que o NavHost
 * do app esteja pronto para consumi-lo (mesmo padrão do `NfcTagBus`).
 */
@Singleton
class PendingQrCodeNavigation @Inject constructor() {
    private val _pendingQrCodeId = MutableStateFlow<String?>(null)
    val pendingQrCodeId: StateFlow<String?> = _pendingQrCodeId.asStateFlow()

    fun set(qrCodeId: String?) {
        if (!qrCodeId.isNullOrBlank()) _pendingQrCodeId.value = qrCodeId
    }

    fun consume(): String? {
        val value = _pendingQrCodeId.value
        _pendingQrCodeId.value = null
        return value
    }
}
