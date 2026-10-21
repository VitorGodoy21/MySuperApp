package com.vfdeginformatica.mysuperapp.presentation.my_super_app

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.vfdeginformatica.mysuperapp.domain.use_case.login.IsLoggedUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.user.RegisterFcmTokenUseCase
import com.vfdeginformatica.mysuperapp.nfc.presentation.NfcForegroundDispatcher
import com.vfdeginformatica.mysuperapp.nfc.presentation.NfcTagBus
import com.vfdeginformatica.mysuperapp.notification.NotificationIntents
import com.vfdeginformatica.mysuperapp.notification.PendingQrCodeNavigation
import com.vfdeginformatica.mysuperapp.notification.requestNotificationPermissionIfNeeded
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var nfcTagBus: NfcTagBus

    @Inject
    lateinit var pendingQrCodeNavigation: PendingQrCodeNavigation

    @Inject
    lateinit var registerFcmTokenUseCase: RegisterFcmTokenUseCase

    @Inject
    lateinit var isLoggedUseCase: IsLoggedUseCase

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermissionIfNeeded(notificationPermissionLauncher)
        pendingQrCodeNavigation.set(NotificationIntents.extractQrCodeId(intent))
        setContent {
            MySuperApp()
        }
    }

    override fun onResume() {
        super.onResume()
        registerFcmTokenIfLogged()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        NfcForegroundDispatcher.extractTag(intent)?.let { tag -> nfcTagBus.tryEmit(tag) }
        pendingQrCodeNavigation.set(NotificationIntents.extractQrCodeId(intent))
    }

    private fun registerFcmTokenIfLogged() {
        lifecycleScope.launch {
            if (isLoggedUseCase().first()) registerFcmTokenUseCase()
        }
    }
}
