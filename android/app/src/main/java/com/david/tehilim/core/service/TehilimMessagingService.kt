package com.david.tehilim.core.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.david.tehilim.MainActivity
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
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(androidx.core.content.ContextCompat.getColor(this, R.color.notification_accent))
            .setContentTitle(n.title ?: getString(R.string.app_name))
            .setContentText(n.body ?: "")
            .setContentIntent(contentIntent(message.data["chainId"]))
            .setAutoCancel(true)
            .build()
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .notify(System.currentTimeMillis().toInt(), notif)
    }

    /**
     * Intent d'ouverture au tap. Si la notif porte un `chainId`, on ouvre
     * directement l'écran de la chaîne via le deep link `tehilim://chain?id=…`
     * (même chemin que les liens de partage / QR) ; sinon on ouvre l'app.
     */
    private fun contentIntent(chainId: String?): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            val cid = chainId?.trim()
            if (!cid.isNullOrEmpty()) {
                action = Intent.ACTION_VIEW
                data = Uri.parse("tehilim://chain?id=$cid")
            }
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getActivity(this, (chainId ?: "").hashCode(), intent, flags)
    }

    private fun localeTag(): String = when (resources.configuration.locales[0].language) {
        "en" -> "en"
        "iw", "he" -> "he"   // l'hébreu est rapporté sous l'ancien code « iw »
        else -> "fr"
    }

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
