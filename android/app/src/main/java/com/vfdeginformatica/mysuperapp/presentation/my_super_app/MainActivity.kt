package com.vfdeginformatica.mysuperapp.presentation.my_super_app

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.vfdeginformatica.mysuperapp.nfc.presentation.NfcForegroundDispatcher
import com.vfdeginformatica.mysuperapp.nfc.presentation.NfcTagBus
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var nfcTagBus: NfcTagBus

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MySuperApp()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        NfcForegroundDispatcher.extractTag(intent)?.let { tag -> nfcTagBus.tryEmit(tag) }
    }
}