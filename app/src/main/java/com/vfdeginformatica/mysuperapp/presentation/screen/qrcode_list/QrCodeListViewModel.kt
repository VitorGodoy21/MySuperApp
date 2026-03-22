package com.vfdeginformatica.mysuperapp.presentation.screen.qrcode_list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.domain.use_case.qrcode.GetQrCodesUseCase
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
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@HiltViewModel
class QrCodeListViewModel @Inject constructor(
    private val getQrCodesUseCase: GetQrCodesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(QrCodeListUiState())
    val uiState: StateFlow<QrCodeListUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<QrCodeListEffect>()
    val effect: SharedFlow<QrCodeListEffect> = _effect.asSharedFlow()

    init {
        getQrCodes()
    }

    fun onEvent(event: QrCodeListEvent) {
        when (event) {
            is QrCodeListEvent.OnSelectQrCode -> {

            }
        }
    }

    private fun getQrCodes() {
        getQrCodesUseCase.invoke().onEach { result ->
            when (result) {
                is Resource.Loading -> {
                    _uiState.value = QrCodeListUiState(isLoading = true)
                }

                is Resource.Success -> {
                    _uiState.value = QrCodeListUiState(qrCodes = result.data ?: emptyList())
                }

                is Resource.Error -> {
                    _uiState.value =
                        QrCodeListUiState(error = result.message ?: "Erro desconhecido")
                }
            }
        }.launchIn(viewModelScope)

    }

}
