package com.vfdeginformatica.qrcodemanager.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.vfdeginformatica.mysuperapp.domain.model.NotificationSettings
import com.vfdeginformatica.qrcodemanager.QrCodeManagerActivity

class QrCodeFcmService : FirebaseMessagingService() {

    companion object {
        const val CHANNEL_ID = "qrcode_notifications"
        private const val CHANNEL_NAME = "Notificacoes QR Code"
        private const val TYPE_ACCESS = "access"
        private const val TYPE_MURAL_COMMENT = "mural_comment"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        saveTokenToFirestore(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val title = message.notification?.title ?: message.data["title"]
        val body = message.notification?.body ?: message.data["body"]
        val type = message.data["type"] ?: ""

        if (title.isNullOrBlank() || body.isNullOrBlank()) return

        loadNotificationSettings { settings ->
            if (shouldShowNotification(settings, type)) {
                showNotification(title, body)
            }
        }
    }

    private fun shouldShowNotification(settings: NotificationSettings, type: String): Boolean {
        if (!settings.enabledAll) return false

        return when (type) {
            TYPE_ACCESS -> settings.enabledAccess
            TYPE_MURAL_COMMENT -> settings.enabledMuralComments
            else -> true
        }
    }

    private fun loadNotificationSettings(onResult: (NotificationSettings) -> Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid.isNullOrEmpty()) {
            onResult(NotificationSettings())
            return
        }

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { snapshot ->
                val settingsMap = snapshot.get("notificationSettings") as? Map<*, *>
                val settings = NotificationSettings(
                    enabledAll = settingsMap?.get("enabledAll") as? Boolean ?: true,
                    enabledAccess = settingsMap?.get("enabledAccess") as? Boolean ?: true,
                    enabledMuralComments =
                        settingsMap?.get("enabledMuralComments") as? Boolean ?: true
                )
                onResult(settings)
            }
            .addOnFailureListener {
                onResult(NotificationSettings())
            }
    }

    private fun saveTokenToFirestore(token: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .set(mapOf("fcmToken" to token), SetOptions.merge())
    }

    private fun showNotification(title: String, body: String) {
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

        val intent = Intent(this, QrCodeManagerActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

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
