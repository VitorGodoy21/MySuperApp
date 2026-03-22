package com.vfdeginformatica.mysuperapp.presentation.screen.qrcode_list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vfdeginformatica.mysuperapp.presentation.screen.qrcode_list.contract.QrCodeListEffect
import com.vfdeginformatica.mysuperapp.presentation.screen.qrcode_list.contract.QrCodeListEvent
import com.vfdeginformatica.mysuperapp.presentation.screen.qrcode_list.contract.QrCodeListUiState
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
class QrCodeListViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(QrCodeListUiState())
    val uiState: StateFlow<QrCodeListUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<QrCodeListEffect>()
    val effect: SharedFlow<QrCodeListEffect> = _effect.asSharedFlow()

    fun onEvent(event: QrCodeListEvent) {
        when (event) {
            is QrCodeListEvent.OnRefresh -> {
                // TODO: Implement refresh logic
            }
        }
    }

    private fun sendEffect(effect: QrCodeListEffect) {
        viewModelScope.launch {
            _effect.emit(effect)
        }
    }
}
