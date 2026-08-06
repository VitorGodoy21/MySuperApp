package com.vfdeginformatica.mysuperapp.nfc.presentation.screen.nfc.contract

import com.vfdeginformatica.mysuperapp.domain.model.QrCode
import com.vfdeginformatica.mysuperapp.nfc.domain.model.NfcTagContent
import com.vfdeginformatica.mysuperapp.nfc.domain.model.NfcWriteContent
import com.vfdeginformatica.mysuperapp.nfc.domain.model.payloadSizeBytes

enum class NfcMode {
    NONE,
    READING,
    WRITING
}

/** Fonte do conteúdo escolhida pelo usuário ao gravar uma tag. */
enum class NfcWriteSource {
    QR_CODE,
    CUSTOM_TEXT
}

/**
 * Capacidade de memória de usuário (em bytes) da tag NFC mínima recomendada
 * pela documentação do projeto (NTAG213). Usada apenas como referência para
 * avisar o usuário no campo de texto antes mesmo de aproximar uma tag; a
 * validação definitiva ocorre com a capacidade real relatada pelo hardware.
 */
const val NTAG213_REFERENCE_CAPACITY_BYTES = 144

data class NfcUiState(
    val isNfcSupported: Boolean = true,
    val isNfcEnabled: Boolean = true,
    val mode: NfcMode = NfcMode.NONE,
    val isWaitingForTag: Boolean = false,
    val pendingLock: Boolean = false,
    val isLoadingQrCodes: Boolean = false,
    val qrCodes: List<QrCode> = emptyList(),
    val writeSource: NfcWriteSource = NfcWriteSource.QR_CODE,
    val selectedQrCode: QrCode? = null,
    val customTextInput: String = "",
    val lastReadContent: NfcTagContent? = null,
    val writeSuccessContent: NfcWriteContent? = null,
    val showLockConfirmation: Boolean = false,
    val isProcessing: Boolean = false,
    val errorMessage: String = ""
) {
    /**
     * Tamanho estimado, em bytes, da mensagem NDEF para o valor personalizado
     * digitado até agora. Usado para avisar o usuário em tempo real, antes de
     * aproximar a tag, caso o conteúdo não caiba na tag mínima recomendada.
     */
    val customTextByteSize: Int
        get() = NfcWriteContent.CustomText(customTextInput).payloadSizeBytes

    /**
     * `true` quando o valor personalizado digitado provavelmente não cabe em
     * uma tag NTAG213 (144 bytes), a opção mínima recomendada pela
     * documentação. Não bloqueia a gravação: tags maiores (NTAG215/216)
     * podem comportar o conteúdo; a validação final é feita pelo hardware.
     */
    val isCustomTextLikelyTooLarge: Boolean
        get() = customTextInput.isNotBlank() && customTextByteSize > NTAG213_REFERENCE_CAPACITY_BYTES
}
