package com.vfdeginformatica.mysuperapp.nfc.presentation.screen.nfc

import android.nfc.Tag
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.domain.model.QrCode
import com.vfdeginformatica.mysuperapp.domain.use_case.qrcode.GetQrCodesUseCase
import com.vfdeginformatica.mysuperapp.nfc.domain.model.NfcWriteContent
import com.vfdeginformatica.mysuperapp.nfc.domain.use_case.LockNfcTagUseCase
import com.vfdeginformatica.mysuperapp.nfc.domain.use_case.ReadNfcTagUseCase
import com.vfdeginformatica.mysuperapp.nfc.domain.use_case.WriteNfcTagUseCase
import com.vfdeginformatica.mysuperapp.nfc.presentation.NfcTagBus
import com.vfdeginformatica.mysuperapp.nfc.presentation.screen.nfc.contract.NfcEffect
import com.vfdeginformatica.mysuperapp.nfc.presentation.screen.nfc.contract.NfcEvent
import com.vfdeginformatica.mysuperapp.nfc.presentation.screen.nfc.contract.NfcMode
import com.vfdeginformatica.mysuperapp.nfc.presentation.screen.nfc.contract.NfcUiState
import com.vfdeginformatica.mysuperapp.nfc.presentation.screen.nfc.contract.NfcWriteSource
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

private const val NFC_SOURCE_PARAM = "source=nfc"

/**
 * ViewModel único (hub) para as ações de NFC: ler/editar (vinculado a um QR
 * Code existente ou a um valor de texto personalizado) e bloquear
 * definitivamente (irreversível), oferecido como passo opcional após uma
 * gravação bem-sucedida ou diretamente pela tela de leitura.
 */
@HiltViewModel
class NfcViewModel @Inject constructor(
    private val getQrCodesUseCase: GetQrCodesUseCase,
    private val readNfcTagUseCase: ReadNfcTagUseCase,
    private val writeNfcTagUseCase: WriteNfcTagUseCase,
    private val lockNfcTagUseCase: LockNfcTagUseCase,
    private val nfcTagBus: NfcTagBus
) : ViewModel() {

    private val _uiState = MutableStateFlow(NfcUiState())
    val uiState: StateFlow<NfcUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<NfcEffect>()
    val effect: SharedFlow<NfcEffect> = _effect.asSharedFlow()

    private var operationJob: Job? = null

    init {
        nfcTagBus.tags.onEach(::onTagDiscovered).launchIn(viewModelScope)
    }

    fun onEvent(event: NfcEvent) {
        when (event) {
            NfcEvent.OnStartReading -> startReading()
            NfcEvent.OnStartWriting -> startWriting()
            NfcEvent.OnCancel -> cancelOperation()
            is NfcEvent.OnSelectWriteSource -> selectWriteSource(event.source)
            is NfcEvent.OnSelectQrCode -> selectQrCode(event.qrCode)
            is NfcEvent.OnCustomTextChanged ->
                _uiState.value = _uiState.value.copy(customTextInput = event.text)
            NfcEvent.OnConfirmCustomText -> confirmCustomText()
            is NfcEvent.OnTagDiscovered -> onTagDiscovered(event.tag)
            NfcEvent.OnConfirmLock -> confirmLock()
            NfcEvent.OnDismissLockConfirmation ->
                _uiState.value = _uiState.value.copy(showLockConfirmation = false)
            NfcEvent.OnEditFromRead -> startWriting()
            NfcEvent.OnLockPermanentlyFromRead -> requestPermanentLockFromRead()
        }
    }

    /** Chamado pela [com.vfdeginformatica.mysuperapp.nfc.presentation.screen.nfc.NfcRoute] ao detectar as capacidades do aparelho. */
    fun updateNfcHardwareStatus(isSupported: Boolean, isEnabled: Boolean) {
        _uiState.value = _uiState.value.copy(isNfcSupported = isSupported, isNfcEnabled = isEnabled)
    }

    private fun startReading() {
        _uiState.value = _uiState.value.copy(
            mode = NfcMode.READING,
            isWaitingForTag = true,
            lastReadContent = null,
            errorMessage = ""
        )
    }

    private fun startWriting() {
        _uiState.value = _uiState.value.copy(
            mode = NfcMode.WRITING,
            writeSource = NfcWriteSource.QR_CODE,
            selectedQrCode = null,
            customTextInput = "",
            writeSuccessContent = null,
            showLockConfirmation = false,
            isWaitingForTag = false,
            errorMessage = ""
        )
        loadQrCodes()
    }

    private fun cancelOperation() {
        operationJob?.cancel()
        _uiState.value = NfcUiState(
            isNfcSupported = _uiState.value.isNfcSupported,
            isNfcEnabled = _uiState.value.isNfcEnabled
        )
    }

    private fun loadQrCodes() {
        getQrCodesUseCase.invoke().onEach { result ->
            when (result) {
                is Resource.Loading -> _uiState.value =
                    _uiState.value.copy(isLoadingQrCodes = true, errorMessage = "")

                is Resource.Success -> _uiState.value = _uiState.value.copy(
                    isLoadingQrCodes = false,
                    qrCodes = result.data ?: emptyList()
                )

                is Resource.Error -> _uiState.value = _uiState.value.copy(
                    isLoadingQrCodes = false,
                    errorMessage = result.message ?: "Erro ao carregar os QR Codes"
                )
            }
        }.launchIn(viewModelScope)
    }

    private fun selectWriteSource(source: NfcWriteSource) {
        _uiState.value = _uiState.value.copy(
            writeSource = source,
            selectedQrCode = null,
            customTextInput = "",
            isWaitingForTag = false,
            errorMessage = ""
        )
    }

    private fun selectQrCode(qrCode: QrCode) {
        _uiState.value = _uiState.value.copy(selectedQrCode = qrCode, isWaitingForTag = true)
    }

    private fun confirmCustomText() {
        val text = _uiState.value.customTextInput.trim()
        if (text.isBlank()) return
        _uiState.value = _uiState.value.copy(customTextInput = text, isWaitingForTag = true)
    }

    private fun onTagDiscovered(tag: Tag) {
        val state = _uiState.value
        if (!state.isWaitingForTag) return

        when {
            state.pendingLock -> lockTag(tag)
            state.mode == NfcMode.READING -> readTag(tag)
            state.mode == NfcMode.WRITING -> writeContentForCurrentSource(state)?.let { writeTag(tag, it) }
        }
    }

    private fun writeContentForCurrentSource(state: NfcUiState): NfcWriteContent? =
        when (state.writeSource) {
            NfcWriteSource.QR_CODE -> state.selectedQrCode?.let { NfcWriteContent.Url(buildNfcUrl(it.staticUrl)) }
            NfcWriteSource.CUSTOM_TEXT -> state.customTextInput.takeIf { it.isNotBlank() }
                ?.let { NfcWriteContent.CustomText(it) }
        }

    private fun readTag(tag: Tag) {
        operationJob?.cancel()
        operationJob = readNfcTagUseCase(tag).onEach { result ->
            when (result) {
                is Resource.Loading -> _uiState.value = _uiState.value.copy(isProcessing = true)

                is Resource.Success -> _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    isWaitingForTag = false,
                    lastReadContent = result.data
                )

                is Resource.Error -> {
                    val message = result.message ?: "Erro ao ler a tag"
                    _uiState.value = _uiState.value.copy(isProcessing = false, errorMessage = message)
                    sendEffect(NfcEffect.ShowToast(message))
                }
            }
        }.launchIn(viewModelScope)
    }

    private fun writeTag(tag: Tag, content: NfcWriteContent) {
        operationJob?.cancel()
        operationJob = writeNfcTagUseCase(tag, content).onEach { result ->
            when (result) {
                is Resource.Loading -> _uiState.value = _uiState.value.copy(isProcessing = true)

                is Resource.Success -> _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    isWaitingForTag = false,
                    writeSuccessContent = content,
                    showLockConfirmation = true
                )

                is Resource.Error -> {
                    val message = result.message ?: "Erro ao gravar a tag"
                    _uiState.value = _uiState.value.copy(isProcessing = false, errorMessage = message)
                    sendEffect(NfcEffect.ShowToast(message))
                }
            }
        }.launchIn(viewModelScope)
    }

    private fun confirmLock() {
        _uiState.value = _uiState.value.copy(
            showLockConfirmation = false,
            pendingLock = true,
            isWaitingForTag = true
        )
        sendEffect(NfcEffect.ShowToast("Aproxime a mesma tag novamente para bloqueá-la"))
    }

    private fun requestPermanentLockFromRead() {
        _uiState.value = _uiState.value.copy(
            pendingLock = true,
            isWaitingForTag = true,
            errorMessage = ""
        )
        sendEffect(NfcEffect.ShowToast("Aproxime a mesma tag novamente para bloqueá-la"))
    }

    private fun lockTag(tag: Tag) {
        operationJob?.cancel()
        operationJob = lockNfcTagUseCase(tag).onEach { result ->
            when (result) {
                is Resource.Loading -> _uiState.value = _uiState.value.copy(isProcessing = true)

                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isProcessing = false,
                        isWaitingForTag = false,
                        pendingLock = false,
                        lastReadContent = _uiState.value.lastReadContent?.copy(isWritable = false)
                    )
                    sendEffect(NfcEffect.ShowToast("Tag bloqueada com sucesso"))
                }

                is Resource.Error -> {
                    val message = result.message ?: "Erro ao bloquear a tag"
                    _uiState.value = _uiState.value.copy(isProcessing = false, errorMessage = message)
                    sendEffect(NfcEffect.ShowToast(message))
                }
            }
        }.launchIn(viewModelScope)
    }

    private fun buildNfcUrl(staticUrl: String): String {
        val separator = if (staticUrl.contains("?")) "&" else "?"
        return "$staticUrl$separator$NFC_SOURCE_PARAM"
    }

    private fun sendEffect(effect: NfcEffect) {
        viewModelScope.launch { _effect.emit(effect) }
    }
}
