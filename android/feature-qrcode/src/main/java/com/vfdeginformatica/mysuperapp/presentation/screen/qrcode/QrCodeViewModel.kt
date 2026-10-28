package com.vfdeginformatica.mysuperapp.presentation.screen.qrcode

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.data.local.ImageCompressor
import com.vfdeginformatica.mysuperapp.domain.model.QrCode
import com.vfdeginformatica.mysuperapp.domain.model.QrCodeType
import com.vfdeginformatica.mysuperapp.domain.use_case.qrcode.DeleteQrImageUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.qrcode.UpdateQrCodeUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.qrcode.UploadQrImageUseCase
import com.vfdeginformatica.mysuperapp.domain.util.QrCodeGenerator
import com.vfdeginformatica.mysuperapp.presentation.screen.qrcode.contract.QrCodeEffect
import com.vfdeginformatica.mysuperapp.presentation.screen.qrcode.contract.QrCodeEvent
import com.vfdeginformatica.mysuperapp.presentation.screen.qrcode.contract.QrCodeUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class QrCodeViewModel(
    private val updateQrCodeUseCase: UpdateQrCodeUseCase,
    private val uploadQrImageUseCase: UploadQrImageUseCase,
    private val deleteQrImageUseCase: DeleteQrImageUseCase,
    private val imageCompressor: ImageCompressor,
    private val qrCodeGenerator: QrCodeGenerator,
    private val bitmapDispatcher: CoroutineDispatcher
) : ViewModel() {

    @Inject
    constructor(
        updateQrCodeUseCase: UpdateQrCodeUseCase,
        uploadQrImageUseCase: UploadQrImageUseCase,
        deleteQrImageUseCase: DeleteQrImageUseCase,
        imageCompressor: ImageCompressor,
        qrCodeGenerator: QrCodeGenerator
    ) : this(
        updateQrCodeUseCase,
        uploadQrImageUseCase,
        deleteQrImageUseCase,
        imageCompressor,
        qrCodeGenerator,
        Dispatchers.Default
    )

    private val _uiState = MutableStateFlow(QrCodeUiState())
    val uiState: StateFlow<QrCodeUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<QrCodeEffect>()
    val effect: SharedFlow<QrCodeEffect> = _effect.asSharedFlow()

    fun onEvent(event: QrCodeEvent) {
        when (event) {
            is QrCodeEvent.OnQrCodeLoaded -> {
                viewModelScope.launch {
                    _uiState.value = _uiState.value.copy(isLoading = true)
                    val bitmap = withContext(bitmapDispatcher) {
                        qrCodeGenerator.generate(event.qrCode.staticUrl)
                    }
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        qrCode = event.qrCode.copy(qrcodeBitmap = bitmap),
                        pendingImageUri = null,
                        pendingDeletePath = ""
                    )
                }
            }

            is QrCodeEvent.OnRedirectUrlChanged -> {
                _uiState.value = _uiState.value.copy(
                    qrCode = _uiState.value.qrCode?.copy(redirectUrl = event.url)
                )
            }

            is QrCodeEvent.OnTypeChanged -> onTypeChanged(event.type)

            is QrCodeEvent.OnTextChanged -> {
                _uiState.value = _uiState.value.copy(
                    qrCode = _uiState.value.qrCode?.copy(text = event.text)
                )
            }

            is QrCodeEvent.OnImagePicked -> {
                _uiState.value = _uiState.value.copy(pendingImageUri = event.uri)
            }

            is QrCodeEvent.OnSaveQrCode -> saveQrCode()
        }
    }

    private fun onTypeChanged(type: QrCodeType) {
        val state = _uiState.value
        val current = state.qrCode ?: return
        val keepsText = type == QrCodeType.TEXT || type == QrCodeType.IMAGE
        val leavingImage = current.type == QrCodeType.IMAGE && type != QrCodeType.IMAGE
        val returningToImage = type == QrCodeType.IMAGE && state.pendingDeletePath.isNotEmpty()

        _uiState.value = state.copy(
            qrCode = current.copy(
                type = type,
                redirectUrl = if (type == QrCodeType.REDIRECT) current.redirectUrl else "",
                text = if (keepsText) current.text else "",
                imageUrl = when {
                    leavingImage -> ""
                    returningToImage -> state.pendingDeleteUrlBackup
                    else -> current.imageUrl
                },
                imagePath = when {
                    leavingImage -> ""
                    returningToImage -> state.pendingDeletePath
                    else -> current.imagePath
                }
            ),
            // Keep the file until the save succeeds; restore it if the user comes back to IMAGE.
            pendingDeletePath = when {
                leavingImage -> current.imagePath
                returningToImage -> ""
                else -> state.pendingDeletePath
            },
            pendingDeleteUrlBackup = when {
                leavingImage -> current.imageUrl
                returningToImage -> ""
                else -> state.pendingDeleteUrlBackup
            },
            pendingImageUri = if (type == QrCodeType.IMAGE) state.pendingImageUri else null
        )
    }

    private fun saveQrCode() {
        val state = _uiState.value
        val qrCode = state.qrCode ?: return

        if (qrCode.type == QrCodeType.IMAGE && qrCode.imageUrl.isBlank() && state.pendingImageUri == null) {
            sendEffect(QrCodeEffect.ShowToast("Selecione uma imagem para o QR Code"))
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = "")

            var toSave = qrCode
            var replacedPath = ""
            val pendingUri = state.pendingImageUri
            if (qrCode.type == QrCodeType.IMAGE && pendingUri != null) {
                val bytes = imageCompressor.compress(pendingUri)
                val uploaded = bytes?.let { data ->
                    uploadQrImageUseCase.invoke(qrCode.id, data)
                        .filter { it !is Resource.Loading }
                        .first()
                        .data
                }
                if (uploaded == null) {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    sendEffect(QrCodeEffect.ShowToast("Não foi possível enviar a imagem. Tente novamente."))
                    return@launch
                }
                replacedPath = qrCode.imagePath
                toSave = qrCode.copy(imageUrl = uploaded.url, imagePath = uploaded.path)
            }

            val result = updateQrCodeUseCase.invoke(toSave.id, toSave)
                .filter { it !is Resource.Loading }
                .first()

            when (result) {
                is Resource.Success -> {
                    cleanupOldImages(replacedPath, state.pendingDeletePath)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        qrCode = toSave,
                        pendingImageUri = null,
                        pendingDeletePath = "",
                        pendingDeleteUrlBackup = ""
                    )
                    sendEffect(QrCodeEffect.ShowToast("QR Code atualizado com sucesso!"))
                }

                else -> {
                    // The new file is orphaned if the document update failed; remove it.
                    if (toSave.imagePath != qrCode.imagePath) deleteQrImageUseCase(toSave.imagePath)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = result.message ?: "Erro ao atualizar"
                    )
                    sendEffect(QrCodeEffect.ShowToast(result.message ?: "Erro desconhecido"))
                }
            }
        }
    }

    private suspend fun cleanupOldImages(vararg paths: String) {
        paths.filter { it.isNotBlank() }.distinct().forEach { deleteQrImageUseCase(it) }
    }

    private fun sendEffect(effect: QrCodeEffect) {
        viewModelScope.launch {
            _effect.emit(effect)
        }
    }
}
