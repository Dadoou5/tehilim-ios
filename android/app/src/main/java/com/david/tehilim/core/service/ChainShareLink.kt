package com.david.tehilim.core.service

import android.content.Context
import android.net.Uri
import com.david.tehilim.R
import com.david.tehilim.core.model.TehilimChain

/**
 * Encodage / décodage du lien de partage d'une **chaîne de Tehilim**.
 * Mirror de PrayerShareLink, mais le payload est minimal : seul l'**id de
 * chaîne** circule (le reste est lu depuis Supabase à l'ouverture).
 *
 * Lien = page de redirection `https://…/c/?id=…` (cliquable WhatsApp) qui
 * rouvre `tehilim://chain?id=…`. App Link sur le host + pathPrefix `/c/`.
 */
object ChainShareLink {

    const val SCHEME = "tehilim"
    const val HOST = "chain"
    const val WEB_BASE_URL = "https://tehilimapp.com/c/"

    /** Lien `https://…/c/?id=<chainId>`. */
    fun uri(chainId: String): Uri =
        Uri.parse(WEB_BASE_URL).buildUpon()
            .appendQueryParameter("id", chainId)
            .build()

    /** Message texte prêt à partager (WhatsApp) : description + lien. */
    fun shareMessage(context: Context, chain: TehilimChain): String {
        val link = uri(chain.id).toString()
        val prefix = context.getString(R.string.chain_share_prefix)
        val cta = context.getString(R.string.chain_share_cta)
        return "$prefix${chain.subjectLine}\n\n$cta\n$link"
    }

    /** True si l'URI ouvre une chaîne : `tehilim://chain` OU App Link `…/c/…`. */
    fun isChainLink(uri: Uri): Boolean {
        if (uri.scheme == SCHEME && uri.host == HOST) return true
        val p = uri.path
        return uri.scheme == "https" && (p == "/c" || p?.startsWith("/c/") == true)
    }

    /** Extrait l'id de chaîne (`?id=…`). Null si absent/invalide. */
    fun chainId(uri: Uri): String? {
        if (!isChainLink(uri)) return null
        return uri.getQueryParameter("id")?.trim()?.takeIf { it.isNotEmpty() }
    }
}
