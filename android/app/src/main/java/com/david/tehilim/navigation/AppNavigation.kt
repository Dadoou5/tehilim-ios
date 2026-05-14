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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.david.tehilim.AppContainer
import com.david.tehilim.features.daily.DailyScreen
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
import com.david.tehilim.features.settings.SettingsScreen

@Composable
fun AppNavigation(container: AppContainer) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val showBottomBar = TopLevelDestination.all.any { it.route == currentRoute }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    TopLevelDestination.all.forEach { dest ->
                        NavigationBarItem(
                            selected = backStackEntry?.destination?.hierarchy?.any { it.route == dest.route } == true,
                            onClick = {
                                navController.navigate(dest.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(iconFor(dest), contentDescription = dest.labelFR) },
                            label = { Text(dest.labelFR) }
                        )
                    }
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
            composable(TopLevelDestination.Home.route) {
                HomeScreen(container = container, navController = navController)
            }
            composable(TopLevelDestination.Psalms.route) {
                PsalmsScreen(container = container, navController = navController)
            }
            composable(TopLevelDestination.Daily.route) {
                DailyScreen(container = container, navController = navController)
            }
            composable(TopLevelDestination.LifeCases.route) {
                LifeCasesScreen(container = container, navController = navController)
            }
            composable(TopLevelDestination.Settings.route) {
                SettingsScreen(container = container)
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

            // Détail Tehilim
            composable(
                Routes.PSALM_DETAIL,
                arguments = listOf(navArgument("psalmId") { type = NavType.IntType })
            ) {
                val psalmId = it.arguments?.getInt("psalmId") ?: 1
                PsalmDetailScreen(container = container, psalmId = psalmId, navController = navController)
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
                PsalmListScreen(container = container, book = null, favoritesOnly = true, navController = navController)
            }

            // Tehilim 119
            composable(Routes.PSALM_119_HOME) {
                Psalm119HomeScreen(container = container, navController = navController)
            }
            composable(
                Routes.PSALM_119_SECTION,
                arguments = listOf(navArgument("index") { type = NavType.IntType })
            ) {
                val index = it.arguments?.getInt("index") ?: 1
                Psalm119SectionScreen(container = container, index = index, navController = navController)
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
