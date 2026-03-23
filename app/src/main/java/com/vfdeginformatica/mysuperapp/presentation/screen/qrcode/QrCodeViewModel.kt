package com.vfdeginformatica.mysuperapp.presentation.screen.qrcode

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
class QrCodeViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(QrCodeUiState())
    val uiState: StateFlow<QrCodeUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<QrCodeEffect>()
    val effect: SharedFlow<QrCodeEffect> = _effect.asSharedFlow()

    fun onEvent(event: QrCodeEvent) {
        when (event) {
            is QrCodeEvent.OnQrCodeGenerated -> {
                _uiState.value = _uiState.value.copy(qrCodeData = event.data)
            }
        }
    }

    private fun sendEffect(effect: QrCodeEffect) {
        viewModelScope.launch {
            _effect.emit(effect)
        }
    }
}
