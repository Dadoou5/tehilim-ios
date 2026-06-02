package com.david.tehilim.core.service

import android.content.Context
import com.david.tehilim.AppContainer
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Récupère le token FCM de l'appareil et l'enregistre dans Supabase pour les
 * notifications de chaîne. No-op si FCM n'est pas configuré (google-services.json
 * absent → aucun FirebaseApp par défaut), l'app reste alors 100 % locale.
 */
object PushRegistrar {
    fun registerToken(context: Context, container: AppContainer) {
        if (FirebaseApp.getApps(context).isEmpty()) return   // pas de config FCM → no-op
        val locale = if (context.resources.configuration.locales[0].language == "en") "en" else "fr"
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            CoroutineScope(Dispatchers.IO).launch {
                runCatching { container.chains.registerDeviceToken(token, locale = locale) }
            }
        }
    }
}
