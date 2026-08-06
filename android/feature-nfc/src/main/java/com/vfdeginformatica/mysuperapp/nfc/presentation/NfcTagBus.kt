package com.vfdeginformatica.mysuperapp.nfc.presentation

import android.nfc.Tag
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Ponte entre `Activity.onNewIntent(intent)` (onde as tags NFC chegam via
 * foreground dispatch) e o [com.vfdeginformatica.mysuperapp.nfc.presentation.screen.nfc.NfcViewModel],
 * que não tem acesso direto ao ciclo de vida da Activity.
 */
@Singleton
class NfcTagBus @Inject constructor() {

    private val _tags = MutableSharedFlow<Tag>(extraBufferCapacity = 1)
    val tags: SharedFlow<Tag> = _tags.asSharedFlow()

    fun tryEmit(tag: Tag) {
        _tags.tryEmit(tag)
    }
}
