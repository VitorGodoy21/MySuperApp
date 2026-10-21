package com.vfdeginformatica.mysuperapp.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.domain.model.NotificationSettings
import com.vfdeginformatica.mysuperapp.domain.use_case.user.GetNotificationSettingsUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.user.RegisterFcmTokenUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Serviço FCM compartilhado entre `:app` e `:app-qrcode`. Cada app o registra no
 * próprio manifest; o toque na notificação abre a launcher activity do app com
 * [NotificationIntents.EXTRA_QR_CODE_ID].
 */
@AndroidEntryPoint
class QrCodeNotificationService : FirebaseMessagingService() {

    companion object {
        const val CHANNEL_ID = "qrcode_notifications"
        private const val CHANNEL_NAME = "Notificações QR Code"
        private const val TAG = "QrCodeNotificationService"
    }

    @Inject
    lateinit var getNotificationSettingsUseCase: GetNotificationSettingsUseCase

    @Inject
    lateinit var registerFcmTokenUseCase: RegisterFcmTokenUseCase

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        scope.launch {
            val result = registerFcmTokenUseCase(token)
            if (result is Resource.Error) Log.w(TAG, "onNewToken: ${result.message}")
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val title = message.notification?.title ?: message.data["title"]
        val body = message.notification?.body ?: message.data["body"]
        val type = message.data["type"] ?: ""
        val qrCodeId = message.data["qrCodeId"]

        if (title.isNullOrBlank() || body.isNullOrBlank()) return

        scope.launch {
            val settings = loadNotificationSettings()
            if (NotificationFilter.shouldShow(settings, type)) {
                showNotification(title, body, qrCodeId)
            }
        }
    }

    private suspend fun loadNotificationSettings(): NotificationSettings {
        return try {
            val result = getNotificationSettingsUseCase()
                .filterNot { it is Resource.Loading }
                .first()
            (result as? Resource.Success)?.data ?: NotificationSettings()
        } catch (e: Exception) {
            Log.w(TAG, "loadNotificationSettings: ${e.message}")
            NotificationSettings()
        }
    }

    private fun showNotification(title: String, body: String, qrCodeId: String?) {
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val intent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            qrCodeId?.let { putExtra(NotificationIntents.EXTRA_QR_CODE_ID, it) }
        }

        val requestCode = qrCodeId?.hashCode() ?: 0
        val pendingIntent = intent?.let {
            PendingIntent.getActivity(
                this,
                requestCode,
                it,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
