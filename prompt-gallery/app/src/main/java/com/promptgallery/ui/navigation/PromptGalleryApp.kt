package com.promptgallery.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.promptgallery.ui.feature.detail.DetailScreen
import com.promptgallery.ui.feature.gallery.GalleryScreen
import com.promptgallery.ui.feature.organize.ImageListScreen
import com.promptgallery.ui.feature.organize.LibraryScreen
import com.promptgallery.ui.feature.search.SearchScreen
import com.promptgallery.ui.feature.settings.SettingsScreen
import com.promptgallery.ui.feature.templates.TemplatesScreen

/**
 * Root composable: hosts the navigation graph and the bottom navigation bar.
 * The bar is shown only on top-level destinations and animates away on detail,
 * list and settings screens for an immersive, content-first feel.
 */
@Composable
fun PromptGalleryApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val showBottomBar = currentRoute in TopLevelDestination.entries.map { it.route }

    Scaffold(
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideInVertically { it },
                exit = slideOutVertically { it },
            ) {
                NavigationBar {
                    TopLevelDestination.entries.forEach { destination ->
                        NavigationBarItem(
                            selected = currentRoute == destination.route,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(Routes.GALLERY) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(destination.icon, contentDescription = destination.label) },
                            label = { Text(destination.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.GALLERY,
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            composable(Routes.GALLERY) {
                GalleryScreen(
                    onOpenImage = { navController.navigate(Routes.detail(it)) },
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                )
            }

            composable(Routes.SEARCH) {
                SearchScreen(onOpenImage = { navController.navigate(Routes.detail(it)) })
            }

            composable(Routes.LIBRARY) {
                LibraryScreen(
                    onOpenCollection = { navController.navigate(Routes.collection(it)) },
                    onOpenFolder = { navController.navigate(Routes.folder(it)) },
                    onOpenTag = { navController.navigate(Routes.tag(it)) },
                )
            }

            composable(Routes.TEMPLATES) { TemplatesScreen() }

            composable(Routes.SETTINGS) {
                SettingsScreen(onBack = { navController.popBackStack() })
            }

            composable(
                route = Routes.DETAIL,
                arguments = listOf(navArgument(Routes.Args.IMAGE_ID) { type = NavType.StringType }),
            ) {
                DetailScreen(
                    onBack = { navController.popBackStack() },
                    onOpenImage = { id ->
                        navController.navigate(Routes.detail(id)) {
                            popUpTo(Routes.DETAIL) { inclusive = true }
                        }
                    },
                )
            }

            composable(
                route = Routes.FOLDER,
                arguments = listOf(navArgument(Routes.Args.FOLDER_ID) { type = NavType.StringType }),
            ) {
                ImageListScreen(
                    onBack = { navController.popBackStack() },
                    onOpenImage = { navController.navigate(Routes.detail(it)) },
                )
            }

            composable(
                route = Routes.COLLECTION,
                arguments = listOf(navArgument(Routes.Args.COLLECTION_ID) { type = NavType.StringType }),
            ) {
                ImageListScreen(
                    onBack = { navController.popBackStack() },
                    onOpenImage = { navController.navigate(Routes.detail(it)) },
                )
            }

            composable(
                route = Routes.TAG,
                arguments = listOf(navArgument(Routes.Args.TAG_ID) { type = NavType.StringType }),
            ) {
                ImageListScreen(
                    onBack = { navController.popBackStack() },
                    onOpenImage = { navController.navigate(Routes.detail(it)) },
                )
            }
        }
    }
}
