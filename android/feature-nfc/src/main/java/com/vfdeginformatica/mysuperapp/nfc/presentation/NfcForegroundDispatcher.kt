package com.vfdeginformatica.mysuperapp.nfc.presentation

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build

/**
 * Encapsula o ciclo de vida do foreground dispatch de NFC para uma
 * [Activity]. Deve ser habilitado em `onResume()` e desabilitado em
 * `onPause()`/`onStop()` da Activity hospedeira. A tag descoberta chega via
 * `onNewIntent(intent)`; use [extractTag] para recuperá-la e publique o
 * resultado em um [NfcTagBus] injetado na própria Activity.
 */
class NfcForegroundDispatcher(private val activity: Activity) {

    private val nfcAdapter: NfcAdapter? = NfcAdapter.getDefaultAdapter(activity)

    val isNfcSupported: Boolean get() = nfcAdapter != null
    val isNfcEnabled: Boolean get() = nfcAdapter?.isEnabled == true

    private val pendingIntent: PendingIntent by lazy {
        val intent = Intent(activity, activity.javaClass).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val mutabilityFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_MUTABLE
        } else {
            0
        }
        PendingIntent.getActivity(activity, 0, intent, mutabilityFlag)
    }

    fun enable() {
        nfcAdapter?.enableForegroundDispatch(activity, pendingIntent, null, null)
    }

    fun disable() {
        nfcAdapter?.disableForegroundDispatch(activity)
    }

    companion object {
        fun extractTag(intent: Intent?): Tag? {
            if (intent == null) return null
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
            }
        }
    }
}
