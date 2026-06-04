package com.david.tehilim.navigation

import androidx.annotation.StringRes
import com.david.tehilim.R

/**
 * Routes de navigation centralisées. Évite les strings magiques dans les calls.
 *
 * Les 5 onglets correspondent à ceux de l'iOS.
 *
 * V1.3.0 — `labelRes` au lieu d'un label FR en dur ; la barre de navigation
 * affiche le bon libellé selon la locale active (FR/EN).
 */
sealed class TopLevelDestination(val route: String, @StringRes val labelRes: Int) {
    data object Home : TopLevelDestination("home", R.string.tab_home)
    data object Psalms : TopLevelDestination("psalms", R.string.tab_psalms)
    data object Daily : TopLevelDestination("daily", R.string.tab_daily)
    data object LifeCases : TopLevelDestination("lifecases", R.string.tab_life_cases)
    data object Settings : TopLevelDestination("settings", R.string.tab_settings)

    companion object {
        val all = listOf(Home, Psalms, Daily, LifeCases, Settings)
    }
}

/** Routes secondaires accessibles par push depuis n'importe quel onglet. */
object Routes {
    const val PSALM_DETAIL = "psalm/{psalmId}?siblings={siblings}"
    fun psalmDetail(id: Int, siblings: List<Int>? = null): String {
        val base = "psalm/$id"
        return if (siblings.isNullOrEmpty()) base
               else "$base?siblings=${siblings.joinToString(",")}"
    }

    const val PSALM_LIST_BOOK = "book/{book}"
    fun psalmListBook(book: Int) = "book/$book"

    /** Ouvre l'onglet Tehilim directement sur un segment précis (0=Livres, 1=Tous, 2=Favoris). */
    fun psalmsWithSegment(segment: Int): String = "psalms?segment=$segment"

    const val PSALM_LIST_FAVORITES = "psalms/favorites"

    const val PSALM_119_HOME = "psalm119"
    const val PSALM_119_SECTION = "psalm119/section/{index}"
    fun psalm119Section(index: Int) = "psalm119/section/$index"

    const val LIFE_CASE_DETAIL = "lifecase/{caseId}"
    fun lifeCaseDetail(caseId: String) = "lifecase/$caseId"

    const val PERSONALIZED_FORM = "personalized/form"
    /** Formulaire en mode édition d'une prière existante. */
    const val PERSONALIZED_EDIT = "personalized/form/edit/{editId}"
    fun personalizedEdit(intentId: String) = "personalized/form/edit/$intentId"
    const val SAVED_PRAYERS = "personalized/saved"
    const val PERSONALIZED_LIST = "personalized/list/{intentId}"
    fun personalizedList(intentId: String) = "personalized/list/$intentId"

    // Chaîne de Tehilim
    const val CHAIN_LIST = "chains"
    const val CHAIN_CREATE = "chains/create"
    const val CHAIN_DETAIL = "chains/detail/{chainId}"
    fun chainDetail(chainId: String) = "chains/detail/$chainId"
    /** Lecture hors-ligne d'une chaîne distribuée (compte rendu local). */
    const val CHAIN_ARCHIVE = "chains/archive/{chainId}"
    fun chainArchive(chainId: String) = "chains/archive/$chainId"
}
