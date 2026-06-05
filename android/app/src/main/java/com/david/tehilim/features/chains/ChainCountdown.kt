package com.david.tehilim.features.chains

/**
 * Compte à rebours partagé des chaînes. Format `HH:MM:SS` (préfixé des jours si
 * besoin) → les secondes défilent à chaque tick, contrairement à un affichage
 * « 5 h 23 min » qui semblait figé.
 */
internal fun chainCountdown(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    val d = total / 86400
    val h = (total % 86400) / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    val hms = "%02d:%02d:%02d".format(h, m, s)
    return if (d > 0) "${d}j $hms" else hms
}
