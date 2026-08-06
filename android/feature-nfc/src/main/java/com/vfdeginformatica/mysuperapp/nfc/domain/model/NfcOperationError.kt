package com.vfdeginformatica.mysuperapp.nfc.domain.model

/**
 * Erros de domínio para as operações de leitura, gravação e bloqueio de
 * tags NFC. Cada variante já carrega uma mensagem amigável para exibição.
 */
sealed class NfcOperationError(val message: String) {
    data object NfcNotSupported :
        NfcOperationError("Este aparelho não possui suporte a NFC.")

    data object NfcDisabled :
        NfcOperationError("O NFC está desativado. Ative-o nas configurações do aparelho.")

    data object TagNotNdef :
        NfcOperationError("Esta tag não é compatível com NDEF.")

    data object TagReadOnly :
        NfcOperationError("Esta tag está bloqueada e não pode ser regravada.")

    data object EmptyTag :
        NfcOperationError("Nenhum conteúdo foi encontrado nesta tag.")

    data object TagLost :
        NfcOperationError("A tag foi removida antes de concluir a operação. Aproxime novamente e tente de novo.")

    data class TagTooSmall(val requiredBytes: Int, val availableBytes: Int) :
        NfcOperationError(
            "A tag não tem espaço suficiente (necessário: $requiredBytes bytes, " +
                "disponível: $availableBytes bytes)."
        )

    data class Unknown(val cause: String) :
        NfcOperationError("Ocorreu um erro inesperado: $cause")
}
