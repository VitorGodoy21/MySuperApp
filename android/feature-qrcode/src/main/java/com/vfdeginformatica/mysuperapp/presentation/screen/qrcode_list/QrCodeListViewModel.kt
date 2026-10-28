package com.vfdeginformatica.mysuperapp.presentation.screen.qrcode_list

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.data.local.ImageCompressor
import com.vfdeginformatica.mysuperapp.domain.model.QrCode
import com.vfdeginformatica.mysuperapp.domain.model.QrCodeType
import com.vfdeginformatica.mysuperapp.domain.use_case.qrcode.CreateQrCodeUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.qrcode.GetQrCodesUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.qrcode.UpdateQrCodeUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.qrcode.UploadQrImageUseCase
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
    private val createQrCodeUseCase: CreateQrCodeUseCase,
    private val updateQrCodeUseCase: UpdateQrCodeUseCase,
    private val uploadQrImageUseCase: UploadQrImageUseCase,
    private val imageCompressor: ImageCompressor
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
                createQrCode(event.type, event.identifier, event.redirectUrl, event.text, event.imageUri)
            }
        }
    }

    private fun createQrCode(
        type: QrCodeType,
        identifier: String,
        redirectUrl: String,
        text: String,
        imageUri: Uri?
    ) {
        if (_uiState.value.isCreating) return

        if (type == QrCodeType.IMAGE && imageUri == null) {
            viewModelScope.launch {
                _effect.emit(QrCodeListEffect.ShowToast("Selecione uma imagem"))
            }
            return
        }

        val trimmedIdentifier = identifier.trim()

        val pendingQrCode = QrCode(
            id = "",
            identifier = trimmedIdentifier,
            redirectUrl = if (type == QrCodeType.REDIRECT) redirectUrl.trim() else "",
            staticUrl = "",
            type = type,
            text = if (type == QrCodeType.TEXT || type == QrCodeType.IMAGE) text.trim() else "",
            userId = ""
        )

        createQrCodeUseCase.invoke(pendingQrCode).onEach { result ->
            when (result) {
                is Resource.Loading -> {
                    _uiState.value = _uiState.value.copy(isCreating = true, error = "")
                }

                is Resource.Success -> {
                    val created = result.data
                    if (type == QrCodeType.IMAGE && created != null && imageUri != null) {
                        attachImage(created, imageUri)
                    } else {
                        finishCreation("QR Code criado com sucesso")
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

    /** Uploads the picked image and stores its URL in the freshly created QR; failures keep the QR without image. */
    private fun attachImage(created: QrCode, imageUri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isUploadingImage = true)
            val bytes = imageCompressor.compress(imageUri)
            if (bytes == null) {
                finishCreation(IMAGE_UPLOAD_FAILED_MESSAGE)
                return@launch
            }
            uploadQrImageUseCase.invoke(created.id, bytes).collect { upload ->
                when (upload) {
                    is Resource.Loading -> Unit
                    is Resource.Success -> {
                        val uploaded = upload.data
                        if (uploaded == null) {
                            finishCreation(IMAGE_UPLOAD_FAILED_MESSAGE)
                            return@collect
                        }
                        val withImage = created.copy(
                            imageUrl = uploaded.url,
                            imagePath = uploaded.path,
                            qrcodeBitmap = null
                        )
                        updateQrCodeUseCase.invoke(created.id, withImage).collect { update ->
                            when (update) {
                                is Resource.Loading -> Unit
                                is Resource.Success -> finishCreation("QR Code criado com sucesso")
                                is Resource.Error -> finishCreation(IMAGE_UPLOAD_FAILED_MESSAGE)
                            }
                        }
                    }

                    is Resource.Error -> finishCreation(IMAGE_UPLOAD_FAILED_MESSAGE)
                }
            }
        }
    }

    private fun finishCreation(message: String) {
        _uiState.value = _uiState.value.copy(isCreating = false, isUploadingImage = false, error = "")
        viewModelScope.launch {
            _effect.emit(QrCodeListEffect.ShowToast(message))
        }
        refresh()
    }

    companion object {
        const val IMAGE_UPLOAD_FAILED_MESSAGE =
            "Imagem não enviada; edite o QR para tentar novamente"
    }
}
