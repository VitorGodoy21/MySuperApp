package com.vfdeginformatica.mysuperapp.presentation.screen.qrcode_list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.domain.model.QrCode
import com.vfdeginformatica.mysuperapp.domain.model.QrCodeType
import com.vfdeginformatica.mysuperapp.domain.use_case.qrcode.CreateQrCodeUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.qrcode.GetQrCodesUseCase
import com.vfdeginformatica.mysuperapp.presentation.screen.qrcode_list.contract.QrCodeListEffect
import com.vfdeginformatica.mysuperapp.presentation.screen.qrcode_list.contract.QrCodeListEvent
import com.vfdeginformatica.mysuperapp.presentation.screen.qrcode_list.contract.QrCodeListUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class QrCodeListViewModel @Inject constructor(
    private val getQrCodesUseCase: GetQrCodesUseCase,
    private val createQrCodeUseCase: CreateQrCodeUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(QrCodeListUiState())
    val uiState: StateFlow<QrCodeListUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<QrCodeListEffect>()
    val effect: SharedFlow<QrCodeListEffect> = _effect.asSharedFlow()

    private var getQrCodesJob: Job? = null

    fun refresh() {
        getQrCodesJob?.cancel()
        getQrCodesJob = getQrCodesUseCase.invoke().onEach { result ->
            when (result) {
                is Resource.Loading -> {
                    _uiState.value = _uiState.value.copy(isLoading = true, error = "")
                }

                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        qrCodes = result.data ?: emptyList(),
                        error = ""
                    )
                }

                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.message ?: "Erro desconhecido"
                    )
                }
            }
        }.launchIn(viewModelScope)
    }

    fun onEvent(event: QrCodeListEvent) {
        when (event) {
            is QrCodeListEvent.OnSelectQrCode -> {
                viewModelScope.launch {
                    _effect.emit(QrCodeListEffect.NavigateToQrCode(event.qrCode))
                }
            }

            is QrCodeListEvent.OnCreateQrCode -> {
                createQrCode(event.type, event.identifier)
            }
        }
    }

    private fun createQrCode(type: QrCodeType, identifier: String) {
        if (_uiState.value.isCreating) return

        val trimmedIdentifier = identifier.trim()

        val pendingQrCode = QrCode(
            id = "",
            identifier = trimmedIdentifier,
            redirectUrl = "",
            staticUrl = "",
            type = type,
            text = "",
            userId = ""
        )

        createQrCodeUseCase.invoke(pendingQrCode).onEach { result ->
            when (result) {
                is Resource.Loading -> {
                    _uiState.value = _uiState.value.copy(isCreating = true, error = "")
                }

                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(isCreating = false, error = "")
                    val createdQrCode = result.data ?: return@onEach
                    viewModelScope.launch {
                        _effect.emit(QrCodeListEffect.ShowToast("QR Code criado com sucesso"))
                        _effect.emit(QrCodeListEffect.NavigateToQrCode(createdQrCode))
                    }
                }

                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isCreating = false,
                        error = result.message ?: "Erro desconhecido"
                    )
                    viewModelScope.launch {
                        _effect.emit(QrCodeListEffect.ShowToast(result.message ?: "Erro desconhecido"))
                    }
                }
            }
        }.launchIn(viewModelScope)
    }
}
