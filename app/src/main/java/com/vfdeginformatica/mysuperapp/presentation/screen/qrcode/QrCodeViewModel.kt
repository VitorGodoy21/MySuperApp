package com.vfdeginformatica.mysuperapp.presentation.screen.qrcode

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.domain.use_case.qrcode.UpdateQrCodeUseCase
import com.vfdeginformatica.mysuperapp.presentation.screen.qrcode.contract.QrCodeEffect
import com.vfdeginformatica.mysuperapp.presentation.screen.qrcode.contract.QrCodeEvent
import com.vfdeginformatica.mysuperapp.presentation.screen.qrcode.contract.QrCodeUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class QrCodeViewModel @Inject constructor(
    private val updateQrCodeUseCase: UpdateQrCodeUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(QrCodeUiState())
    val uiState: StateFlow<QrCodeUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<QrCodeEffect>()
    val effect: SharedFlow<QrCodeEffect> = _effect.asSharedFlow()

    fun onEvent(event: QrCodeEvent) {
        when (event) {
            is QrCodeEvent.OnQrCodeLoaded -> {
                _uiState.value = _uiState.value.copy(qrCode = event.qrCode)
            }

            is QrCodeEvent.OnRedirectUrlChanged -> {
                _uiState.value = _uiState.value.copy(
                    qrCode = _uiState.value.qrCode?.copy(redirectUrl = event.url)
                )
            }

            is QrCodeEvent.OnSaveQrCode -> {
                saveQrCode()
            }
        }
    }

    private fun saveQrCode() {
        val qrCode = _uiState.value.qrCode ?: return

        viewModelScope.launch {
            updateQrCodeUseCase.invoke(qrCode.id, qrCode).collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        _uiState.value = _uiState.value.copy(isLoading = true)
                    }

                    is Resource.Success -> {
                        _uiState.value = _uiState.value.copy(isLoading = false)
                        sendEffect(QrCodeEffect.ShowToast("QR Code atualizado com sucesso!"))
                    }

                    is Resource.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = resource.message ?: "Erro ao atualizar"
                        )
                        sendEffect(QrCodeEffect.ShowToast(resource.message ?: "Erro desconhecido"))
                    }
                }
            }
        }
    }

    private fun sendEffect(effect: QrCodeEffect) {
        viewModelScope.launch {
            _effect.emit(effect)
        }
    }
}


