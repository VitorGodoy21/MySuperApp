package com.vfdeginformatica.mysuperapp.presentation.my_super_app

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vfdeginformatica.mysuperapp.Screen
import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.domain.model.QrCode
import com.vfdeginformatica.mysuperapp.domain.use_case.qrcode.GetQrCodeByIdUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.user.ProtectedNavigationResult
import com.vfdeginformatica.mysuperapp.domain.use_case.user.ResolveProtectedNavigationUseCase
import com.vfdeginformatica.mysuperapp.notification.PendingQrCodeNavigation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface NotificationDeepLinkEffect {
    data class OpenQrCode(val qrCode: QrCode) : NotificationDeepLinkEffect
    data class Navigate(val route: String) : NotificationDeepLinkEffect
    data class ShowToast(val message: String) : NotificationDeepLinkEffect
}

/**
 * Trata o toque em uma notificação de QR Code no `:app`: exige biometria (mesma
 * proteção do Smart Tags), carrega o QR e navega para o detalhe. Se o QR não for
 * encontrado, cai na lista de QR Codes com aviso; se a biometria falhar, só avisa.
 */
@HiltViewModel
class NotificationDeepLinkViewModel @Inject constructor(
    private val pendingQrCodeNavigation: PendingQrCodeNavigation,
    private val resolveProtectedNavigationUseCase: ResolveProtectedNavigationUseCase,
    private val getQrCodeByIdUseCase: GetQrCodeByIdUseCase
) : ViewModel() {

    val pendingQrCodeId: StateFlow<String?> = pendingQrCodeNavigation.pendingQrCodeId

    private val _effect = MutableSharedFlow<NotificationDeepLinkEffect>()
    val effect: SharedFlow<NotificationDeepLinkEffect> = _effect.asSharedFlow()

    fun consumePending(activity: FragmentActivity?) {
        val qrCodeId = pendingQrCodeNavigation.consume() ?: return
        viewModelScope.launch {
            val auth = resolveProtectedNavigationUseCase(
                route = Screen.QrCodeListScreen.route,
                passwordRequired = true,
                activity = activity
            ).first()

            when (auth) {
                is ProtectedNavigationResult.Denied ->
                    _effect.emit(NotificationDeepLinkEffect.ShowToast(auth.message))

                is ProtectedNavigationResult.Allowed -> {
                    val result = getQrCodeByIdUseCase(qrCodeId)
                        .filterNot { it is Resource.Loading }
                        .first()
                    val qrCode = (result as? Resource.Success)?.data
                    if (qrCode != null) {
                        _effect.emit(NotificationDeepLinkEffect.OpenQrCode(qrCode))
                    } else {
                        _effect.emit(
                            NotificationDeepLinkEffect.ShowToast(
                                result.message ?: "QR Code não encontrado"
                            )
                        )
                        _effect.emit(NotificationDeepLinkEffect.Navigate(auth.route))
                    }
                }
            }
        }
    }
}
