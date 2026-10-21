package com.vfdeginformatica.mysuperapp.notification

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat

object NotificationIntents {
    const val EXTRA_QR_CODE_ID = "extra_qr_code_id"

    fun extractQrCodeId(intent: Intent?): String? = intent?.getStringExtra(EXTRA_QR_CODE_ID)
}

/** Solicita `POST_NOTIFICATIONS` no Android 13+ quando ainda não concedida. */
fun Activity.requestNotificationPermissionIfNeeded(launcher: ActivityResultLauncher<String>) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val granted = ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.POST_NOTIFICATIONS
    ) == PackageManager.PERMISSION_GRANTED
    if (!granted) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
}
