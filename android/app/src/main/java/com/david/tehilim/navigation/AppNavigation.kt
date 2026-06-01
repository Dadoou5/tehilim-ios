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

    // Import d'une prière partagée (`tehilim://prayer?...`) en attente de
    // confirmation. Géré séparément du routing navController car aucune
    // destination ne matche ce host : on présente un AlertDialog d'aperçu.
    var pendingImport by remember {
        mutableStateOf<com.david.tehilim.core.service.PrayerShareLink.Payload?>(null)
    }
    // URI déjà traitée : évite de ré-importer à chaque recomposition / rotation.
    var lastHandledPrayerUri by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(activity?.intent) {
        val data = activity?.intent?.data
        // 1) Prière partagée — schéma custom `tehilim://prayer` OU App Link
        //    `https://dadoou5.github.io/p/…`. Traitée au cold-start ET via
        //    onNewIntent. Aucun navDeepLink ne route ces liens → pas de risque
        //    de double-routing de back stack.
        if (data != null &&
            com.david.tehilim.core.service.PrayerShareLink.isPrayerLink(data)
        ) {
            val uriStr = data.toString()
            if (uriStr != lastHandledPrayerUri) {
                lastHandledPrayerUri = uriStr
                pendingImport = com.david.tehilim.core.service.PrayerShareLink.payload(data)
            }
            return@LaunchedEffect
        }
        // 1bis) Chaîne de Tehilim — `tehilim://chain?id=…` OU App Link `…/c/?id=…`.
        if (data != null && com.david.tehilim.core.service.ChainShareLink.isChainLink(data)) {
            val uriStr = data.toString()
            val cid = com.david.tehilim.core.service.ChainShareLink.chainId(data)
            if (cid != null && uriStr != lastHandledPrayerUri) {
                lastHandledPrayerUri = uriStr
                navController.navigate(Routes.chainDetail(cid))
            }
            return@LaunchedEffect
        }
        // 2) Deep links de navigation (widget/notif) — skip la 1ʳᵉ invocation.
        if (!initialIntentSeen) {
            initialIntentSeen = true
            return@LaunchedEffect
        }
        if (data != null) {
            navController.handleDeepLink(activity.intent)
        }
    }

    // AlertDialog d'aperçu d'import — overlay au-dessus du Scaffold.
    pendingImport?.let { payload ->
        val alreadyExists = container.savedPrayers.findExisting(
            relativeFirstName = payload.relativeFirstName,
            relationType = payload.relationType,
            motherFirstName = payload.motherFirstName
        ) != null
        com.david.tehilim.features.personalized.PrayerImportDialog(
            payload = payload,
            alreadyExists = alreadyExists,
            onConfirm = {
                // addOrFindExisting : ajoute, ou retourne le doublon existant.
                // Dans les deux cas on ouvre la prière → « Ouvrir » fonctionne
                // aussi pour un doublon.
                val saved = container.savedPrayers.addOrFindExisting(
                    com.david.tehilim.core.service.PrayerShareLink.makeIntent(payload)
                )
                pendingImport = null
                navController.navigate(Routes.personalizedList(saved.id))
            },
            onDismiss = { pendingImport = null }
        )
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
                            // V1.4 — fix brute-force pour l'onglet Accueil
                            // (cold-start via widget/notif sur Daily) :
                            // - `popBackStack(home, inclusive=false)` essaie
                            //   de revenir à home s'il est déjà dans la stack
                            //   (cas chaud : app déjà ouverte, user a vu home).
                            // - Si home n'est pas dans la stack (cas cold start
                            //   via deep link tehilim://daily, où le synthetic
                            //   back stack n'inclut pas forcément home),
                            //   `popped == false` → on `navigate("home")` en
                            //   reset complet de la stack via `popUpTo(graph.id)
                            //   inclusive=true`.
                            // Pour les autres onglets, le pattern standard
                            // popUpTo(home.route) suffit puisque home est
                            // toujours la base.
                            if (dest == TopLevelDestination.Home) {
                                val popped = navController.popBackStack(
                                    TopLevelDestination.Home.route,
                                    inclusive = false
                                )
                                if (!popped) {
                                    navController.navigate(TopLevelDestination.Home.route) {
                                        popUpTo(navController.graph.id) {
                                            inclusive = true
                                            saveState = false
                                        }
                                        launchSingleTop = true
                                    }
                                }
                            } else {
                                navController.navigate(dest.route) {
                                    popUpTo(TopLevelDestination.Home.route) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
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
            composable(
                Routes.PERSONALIZED_EDIT,
                arguments = listOf(navArgument("editId") { type = NavType.StringType })
            ) {
                val editId = it.arguments?.getString("editId")
                PersonalizedReadingFormScreen(
                    container = container,
                    navController = navController,
                    editIntentId = editId
                )
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

            // Chaîne de Tehilim
            composable(Routes.CHAIN_LIST) {
                com.david.tehilim.features.chains.MyChainsScreen(container = container, navController = navController)
            }
            composable(Routes.CHAIN_CREATE) {
                com.david.tehilim.features.chains.CreateChainScreen(
                    container = container,
                    onBack = { navController.popBackStack() },
                    onCreated = { id ->
                        navController.navigate(Routes.chainDetail(id)) {
                            popUpTo(Routes.CHAIN_CREATE) { inclusive = true }
                        }
                    }
                )
            }
            composable(
                Routes.CHAIN_DETAIL,
                arguments = listOf(navArgument("chainId") { type = NavType.StringType })
            ) {
                val id = it.arguments?.getString("chainId") ?: ""
                com.david.tehilim.features.chains.ChainDetailScreen(container = container, chainId = id, navController = navController)
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
