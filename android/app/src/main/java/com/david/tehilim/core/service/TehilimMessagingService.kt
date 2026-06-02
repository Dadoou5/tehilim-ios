package com.david.tehilim.core.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.david.tehilim.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Réception FCM des notifications de chaîne (messagerie push ; la base reste
 * Supabase). Affichage manuel quand l'app est au premier plan ; en arrière-plan,
 * FCM affiche tout seul la payload `notification`. Met aussi à jour le token
 * (rotation) dans Supabase.
 */
class TehilimMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        val svc = ChainService(applicationContext)
        val locale = localeTag()
        CoroutineScope(Dispatchers.IO).launch {
            runCatching { svc.registerDeviceToken(token, locale = locale) }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val n = message.notification ?: return
        ensureChannel()
        val notif = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(n.title ?: getString(R.string.app_name))
            .setContentText(n.body ?: "")
            .setAutoCancel(true)
            .build()
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .notify(System.currentTimeMillis().toInt(), notif)
    }

    private fun localeTag(): String =
        if (resources.configuration.locales[0].language == "en") "en" else "fr"

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (mgr.getNotificationChannel(CHANNEL_ID) == null) {
                mgr.createNotificationChannel(
                    NotificationChannel(CHANNEL_ID, "Chaînes de Tehilim", NotificationManager.IMPORTANCE_DEFAULT)
                )
            }
        }
    }

    companion object { const val CHANNEL_ID = "tehilim_chain" }
}
