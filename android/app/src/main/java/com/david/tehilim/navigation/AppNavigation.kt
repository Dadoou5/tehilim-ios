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
import android.app.Activity
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
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

    // V1.4 — re-route les deep links arrivés via `onNewIntent` (cas widget/
    // notif tappé alors que l'app était déjà ouverte). Avec
    // `launchMode="singleTop"`, l'Activity n'est pas recréée → NavHost a
    // déjà fait son routing initial → le nouvel Intent serait ignoré sans
    // ce relais explicite.
    //
    // **Skip de la 1ʳᵉ invocation** : sur le cold-start, NavHost lit lui-même
    // l'`Activity.intent` initial et route correctement. Re-appeler
    // `handleDeepLink` dupliquerait les entrées de back stack (stack
    // [home, daily] devient [home, daily, home, daily]) — ce qui cassait
    // le bouton Accueil de la bottom bar (popUpTo trouvait le mauvais
    // « home »). On consomme silencieusement le 1ᵉʳ tick.
    val activity = LocalContext.current as? Activity
    var initialIntentSeen by remember { mutableStateOf(false) }
    LaunchedEffect(activity?.intent) {
        if (!initialIntentSeen) {
            initialIntentSeen = true
            return@LaunchedEffect
        }
        activity?.intent?.let { intent ->
            if (intent.data != null) {
                navController.handleDeepLink(intent)
            }
        }
    }

    // V1.2.5 — La barre de navigation reste toujours visible, comme la TabView
    // iOS qui enveloppe chaque NavigationStack. Avant V1.2.5, on la cachait sur
    // les écrans de détail (PsalmDetail, LifeCaseDetail, Search…) ce qui
    // empêchait l'utilisateur de switcher d'onglet sans revenir en arrière.
    // V1.4 — taille de police adaptative pour les labels de la barre du bas,
    // calculée à partir de la largeur d'écran divisée par 5 onglets. Le label
    // le plus long (« Cas de la vie », 12 chars) doit tenir sur une seule
    // ligne sans tronquer sur les écrans les plus étroits (320–360dp).
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val tabWidthDp = screenWidthDp / 5
    val labelFontSp = when {
        tabWidthDp >= 78 -> 11   // grands écrans (Pixel 9, S24 Ultra…)
        tabWidthDp >= 70 -> 10   // milieu de gamme (Pixel 7a, ~411dp)
        tabWidthDp >= 60 -> 9    // compact (Pixel 4a, ~393dp)
        else -> 8                // très étroit (320dp, devices vintage)
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                TopLevelDestination.all.forEach { dest ->
                    NavigationBarItem(
                        // V1.2.12 — strip query string avant de comparer, sinon une
                        // route comme "psalms?segment={segment}" ne matcherait pas
                        // "psalms" en littéral.
                        selected = backStackEntry?.destination?.hierarchy
                            ?.any { (it.route?.substringBefore('?') ?: "") == dest.route } == true,
                        onClick = {
                            // V1.4 — `findStartDestination().id` au lieu de
                            // `Home.route` : robuste au cold-start via deep link
                            // (widget/notif). Quand l'app ouvre directement sur
                            // Daily, "home" n'est pas dans la back stack, donc
                            // popUpTo("home") ne pouvait pas pop quoi que ce
                            // soit → bouton Accueil cassé. La vraie start
                            // destination du graph est toujours présente
                            // virtuellement, popUpTo dessus marche universellement.
                            navController.navigate(dest.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(iconFor(dest), contentDescription = null) },
                        // V1.4 — label visible en permanence (alwaysShowLabel)
                        // mirror du comportement iOS TabView. Taille adaptative
                        // + maxLines=1 + softWrap=false pour garantir une ligne
                        // unique, ellipsize si vraiment trop étroit.
                        label = {
                            Text(
                                text = stringResource(dest.labelRes),
                                fontSize = labelFontSp.sp,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        alwaysShowLabel = true
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
                "${TopLevelDestination.Psalms.route}?segment={segment}",
                arguments = listOf(
                    navArgument("segment") {
                        type = NavType.IntType; defaultValue = 0
                    }
                ),
                deepLinks = listOf(navDeepLink { uriPattern = "tehilim://psalms" })
            ) {
                val initialSegment = it.arguments?.getInt("segment") ?: 0
                PsalmsScreen(
                    container = container,
                    navController = navController,
                    initialSegment = initialSegment
                )
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
