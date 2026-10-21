package com.vfdeginformatica.mysuperapp.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.domain.model.QrCode
import com.vfdeginformatica.mysuperapp.domain.use_case.qrcode.GetQrCodeByIdUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface QrCodeDeepLinkEffect {
    data class OpenQrCode(val qrCode: QrCode) : QrCodeDeepLinkEffect
    data class NotFound(val message: String) : QrCodeDeepLinkEffect
}

/**
 * Resolve o `qrCodeId` pendente de uma notificação para um [QrCode] carregado,
 * emitindo o efeito correspondente para o NavHost navegar.
 */
@HiltViewModel
class QrCodeDeepLinkViewModel @Inject constructor(
    private val pendingQrCodeNavigation: PendingQrCodeNavigation,
    private val getQrCodeByIdUseCase: GetQrCodeByIdUseCase
) : ViewModel() {

    val pendingQrCodeId: StateFlow<String?> = pendingQrCodeNavigation.pendingQrCodeId

    private val _effect = MutableSharedFlow<QrCodeDeepLinkEffect>()
    val effect: SharedFlow<QrCodeDeepLinkEffect> = _effect.asSharedFlow()

    /** Consome o id pendente e carrega o QR Code; sem id pendente é no-op. */
    fun consumePending() {
        val id = pendingQrCodeNavigation.consume() ?: return
        viewModelScope.launch {
            getQrCodeByIdUseCase(id).collect { resource ->
                when (resource) {
                    is Resource.Success -> resource.data?.let {
                        _effect.emit(QrCodeDeepLinkEffect.OpenQrCode(it))
                    }
                    is Resource.Error -> _effect.emit(
                        QrCodeDeepLinkEffect.NotFound(resource.message ?: "QR Code não encontrado")
                    )
                    is Resource.Loading -> Unit
                }
            }
        }
    }
}
