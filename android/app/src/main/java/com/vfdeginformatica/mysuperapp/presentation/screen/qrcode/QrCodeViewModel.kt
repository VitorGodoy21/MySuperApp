package com.vfdeginformatica.mysuperapp.presentation.screen.qrcode

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.domain.model.QrCodeType
import com.vfdeginformatica.mysuperapp.domain.use_case.qrcode.UpdateQrCodeUseCase
import com.vfdeginformatica.mysuperapp.domain.util.QrCodeGenerator
import com.vfdeginformatica.mysuperapp.presentation.screen.qrcode.contract.QrCodeEffect
import com.vfdeginformatica.mysuperapp.presentation.screen.qrcode.contract.QrCodeEvent
import com.vfdeginformatica.mysuperapp.presentation.screen.qrcode.contract.QrCodeUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class QrCodeViewModel @Inject constructor(
    private val updateQrCodeUseCase: UpdateQrCodeUseCase,
    private val qrCodeGenerator: QrCodeGenerator
) : ViewModel() {

    private val _uiState = MutableStateFlow(QrCodeUiState())
    val uiState: StateFlow<QrCodeUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<QrCodeEffect>()
    val effect: SharedFlow<QrCodeEffect> = _effect.asSharedFlow()

    fun onEvent(event: QrCodeEvent) {
        when (event) {
            is QrCodeEvent.OnQrCodeLoaded -> {
                viewModelScope.launch {
                    _uiState.value = _uiState.value.copy(isLoading = true)
                    val bitmap = withContext(Dispatchers.Default) {
                        qrCodeGenerator.generate(event.qrCode.staticUrl)
                    }
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        qrCode = event.qrCode.copy(qrcodeBitmap = bitmap)
                    )
                }
            }

            is QrCodeEvent.OnRedirectUrlChanged -> {
                _uiState.value = _uiState.value.copy(
                    qrCode = _uiState.value.qrCode?.copy(redirectUrl = event.url)
                )
            }

            is QrCodeEvent.OnTypeChanged -> {
                _uiState.value = _uiState.value.copy(
                    qrCode = _uiState.value.qrCode?.copy(
                        type = event.type,
                        redirectUrl = if (event.type == QrCodeType.TEXT) "" else _uiState.value.qrCode?.redirectUrl
                            ?: "",
                        text = if (event.type == QrCodeType.REDIRECT) "" else _uiState.value.qrCode?.text
                            ?: ""
                    )
                )
            }

            is QrCodeEvent.OnTextChanged -> {
                _uiState.value = _uiState.value.copy(
                    qrCode = _uiState.value.qrCode?.copy(text = event.text)
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


