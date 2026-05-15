package com.david.tehilim.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import androidx.navigation.NavType
import com.david.tehilim.AppContainer
import com.david.tehilim.features.daily.DailyScreen
import com.david.tehilim.features.favorites.FavoritesScreen
import com.david.tehilim.features.home.HomeScreen
import com.david.tehilim.features.lifecases.LifeCaseDetailScreen
import com.david.tehilim.features.lifecases.LifeCasesScreen
import com.david.tehilim.features.personalized.PersonalizedReadingFormScreen
import com.david.tehilim.features.personalized.PersonalizedReadingListScreen
import com.david.tehilim.features.personalized.SavedPrayersScreen
import com.david.tehilim.features.psalm119.Psalm119HomeScreen
import com.david.tehilim.features.psalm119.Psalm119SectionScreen
import com.david.tehilim.features.psalms.PsalmDetailScreen
import com.david.tehilim.features.psalms.PsalmListScreen
import com.david.tehilim.features.psalms.PsalmsScreen
import com.david.tehilim.features.search.SearchScreen
import com.david.tehilim.features.settings.AboutContentScreen
import com.david.tehilim.features.settings.AboutPrivacyScreen
import com.david.tehilim.features.settings.AccessibilityScreen
import com.david.tehilim.features.settings.SettingsScreen

@Composable
fun AppNavigation(container: AppContainer) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()

    // V1.2.5 — La barre de navigation reste toujours visible, comme la TabView
    // iOS qui enveloppe chaque NavigationStack. Avant V1.2.5, on la cachait sur
    // les écrans de détail (PsalmDetail, LifeCaseDetail, Search…) ce qui
    // empêchait l'utilisateur de switcher d'onglet sans revenir en arrière.
    Scaffold(
        bottomBar = {
            NavigationBar {
                TopLevelDestination.all.forEach { dest ->
                    NavigationBarItem(
                        selected = backStackEntry?.destination?.hierarchy?.any { it.route == dest.route } == true,
                        onClick = {
                            // V1.2.11 — popUpTo par route nommée plutôt que par
                            // startDestination().id. Compose Navigation peut perdre
                            // le lien vers la start destination quand la back stack
                            // contient des routes avec query params (cas réel : Psalm
                            // 119 section dont la route est
                            // "psalm119/section/{index}?intentId={intentId}&pos={pos}").
                            // Avec popUpTo(Home.route) ça marche dans tous les cas.
                            navController.navigate(dest.route) {
                                popUpTo(TopLevelDestination.Home.route) {
                                    inclusive = false
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        // Icône seule, sans label. Le contentDescription garde
                        // l'accessibilité TalkBack via le nom de l'onglet.
                        icon = { Icon(iconFor(dest), contentDescription = dest.labelFR) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = TopLevelDestination.Home.route,
            modifier = Modifier.padding(padding)
        ) {
            // Top-level
            composable(
                TopLevelDestination.Home.route,
                deepLinks = listOf(navDeepLink { uriPattern = "tehilim://home" })
            ) {
                HomeScreen(container = container, navController = navController)
            }
            composable(
                TopLevelDestination.Psalms.route,
                deepLinks = listOf(navDeepLink { uriPattern = "tehilim://psalms" })
            ) {
                PsalmsScreen(container = container, navController = navController)
            }
            composable(
                TopLevelDestination.Daily.route,
                deepLinks = listOf(navDeepLink { uriPattern = "tehilim://daily" })
            ) {
                DailyScreen(container = container, navController = navController)
            }
            composable(
                TopLevelDestination.LifeCases.route,
                deepLinks = listOf(navDeepLink { uriPattern = "tehilim://lifecases" })
            ) {
                LifeCasesScreen(container = container, navController = navController)
            }
            composable(
                TopLevelDestination.Settings.route,
                deepLinks = listOf(navDeepLink { uriPattern = "tehilim://settings" })
            ) {
                SettingsScreen(container = container, navController = navController)
            }

            // Recherche
            composable("search") {
                SearchScreen(container = container, navController = navController)
            }

            // About
            composable("about/content") {
                AboutContentScreen(navController = navController)
            }
            composable("about/privacy") {
                AboutPrivacyScreen(navController = navController)
            }
            composable("about/accessibility") {
                AccessibilityScreen(navController = navController)
            }

            // Détail Tehilim — siblings optionnels pour prev/next contextuel
            // (life case, favoris, journalier, liste filtrée par livre, etc.)
            composable(
                Routes.PSALM_DETAIL,
                arguments = listOf(
                    navArgument("psalmId") { type = NavType.IntType },
                    navArgument("siblings") {
                        type = NavType.StringType; nullable = true; defaultValue = null
                    }
                )
            ) {
                val psalmId = it.arguments?.getInt("psalmId") ?: 1
                val siblings = it.arguments?.getString("siblings")
                    ?.split(",")
                    ?.mapNotNull { s -> s.trim().toIntOrNull() }
                    ?.takeIf { list -> list.isNotEmpty() }
                PsalmDetailScreen(
                    container = container,
                    psalmId = psalmId,
                    navController = navController,
                    siblings = siblings
                )
            }

            // Listes Tehilim
            composable(
                Routes.PSALM_LIST_BOOK,
                arguments = listOf(navArgument("book") { type = NavType.IntType })
            ) {
                val book = it.arguments?.getInt("book") ?: 1
                PsalmListScreen(container = container, book = book, navController = navController)
            }
            composable(Routes.PSALM_LIST_ALL) {
                PsalmListScreen(container = container, book = null, navController = navController)
            }
            composable(Routes.PSALM_LIST_FAVORITES) {
                FavoritesScreen(container = container, navController = navController)
            }

            // Tehilim 119
            composable(Routes.PSALM_119_HOME) {
                Psalm119HomeScreen(container = container, navController = navController)
            }
            composable(
                "${Routes.PSALM_119_SECTION}?intentId={intentId}&pos={pos}",
                arguments = listOf(
                    navArgument("index") { type = NavType.IntType },
                    navArgument("intentId") {
                        type = NavType.StringType; nullable = true; defaultValue = null
                    },
                    navArgument("pos") {
                        type = NavType.IntType; defaultValue = -1
                    }
                )
            ) {
                val index = it.arguments?.getInt("index") ?: 1
                val intentId = it.arguments?.getString("intentId")
                val pos = it.arguments?.getInt("pos") ?: -1
                Psalm119SectionScreen(
                    container = container,
                    index = index,
                    navController = navController,
                    savedIntentId = intentId,
                    sequencePosition = if (pos >= 0) pos else null
                )
            }

            // Cas de la vie
            composable(
                Routes.LIFE_CASE_DETAIL,
                arguments = listOf(navArgument("caseId") { type = NavType.StringType })
            ) {
                val caseId = it.arguments?.getString("caseId") ?: ""
                LifeCaseDetailScreen(container = container, caseId = caseId, navController = navController)
            }

            // Lelouy Nichmat
            composable(Routes.PERSONALIZED_FORM) {
                PersonalizedReadingFormScreen(container = container, navController = navController)
            }
            composable(Routes.SAVED_PRAYERS) {
                SavedPrayersScreen(container = container, navController = navController)
            }
            composable(
                Routes.PERSONALIZED_LIST,
                arguments = listOf(navArgument("intentId") { type = NavType.StringType })
            ) {
                val id = it.arguments?.getString("intentId") ?: ""
                PersonalizedReadingListScreen(container = container, intentId = id, navController = navController)
            }
        }
    }
}

private fun iconFor(dest: TopLevelDestination) = when (dest) {
    TopLevelDestination.Home -> Icons.Outlined.Home
    TopLevelDestination.Psalms -> Icons.Outlined.AutoStories
    TopLevelDestination.Daily -> Icons.Outlined.WbSunny
    TopLevelDestination.LifeCases -> Icons.Outlined.Favorite
    TopLevelDestination.Settings -> Icons.Outlined.Settings
}
