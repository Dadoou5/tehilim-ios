package com.david.tehilim.navigation

/**
 * Routes de navigation centralisées. Évite les strings magiques dans les calls.
 *
 * Les 5 onglets correspondent à ceux de l'iOS.
 */
sealed class TopLevelDestination(val route: String, val labelFR: String) {
    data object Home : TopLevelDestination("home", "Accueil")
    data object Psalms : TopLevelDestination("psalms", "Tehilim")
    data object Daily : TopLevelDestination("daily", "Aujourd'hui")
    data object LifeCases : TopLevelDestination("lifecases", "Cas de la vie")
    data object Settings : TopLevelDestination("settings", "Réglages")

    companion object {
        val all = listOf(Home, Psalms, Daily, LifeCases, Settings)
    }
}

/** Routes secondaires accessibles par push depuis n'importe quel onglet. */
object Routes {
    const val PSALM_DETAIL = "psalm/{psalmId}"
    fun psalmDetail(id: Int) = "psalm/$id"

    const val PSALM_LIST_BOOK = "book/{book}"
    fun psalmListBook(book: Int) = "book/$book"

    const val PSALM_LIST_ALL = "psalms/all"
    const val PSALM_LIST_FAVORITES = "psalms/favorites"

    const val PSALM_119_HOME = "psalm119"
    const val PSALM_119_SECTION = "psalm119/section/{index}"
    fun psalm119Section(index: Int) = "psalm119/section/$index"

    const val LIFE_CASE_DETAIL = "lifecase/{caseId}"
    fun lifeCaseDetail(caseId: String) = "lifecase/$caseId"

    const val PERSONALIZED_FORM = "personalized/form"
    const val SAVED_PRAYERS = "personalized/saved"
    const val PERSONALIZED_LIST = "personalized/list/{intentId}"
    fun personalizedList(intentId: String) = "personalized/list/$intentId"
}
