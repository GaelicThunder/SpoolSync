package dev.gaelicthunder.spoolsync.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

@Composable
fun SpoolSyncNavigation(viewModel: SpoolSyncViewModel = viewModel()) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            SpoolSyncApp(
                viewModel = viewModel,
                onFilamentClick = { filamentId ->
                    navController.navigate("detail/$filamentId")
                }
            )
        }
        composable(
            route = "detail/{filamentId}",
            arguments = listOf(navArgument("filamentId") { type = NavType.LongType })
        ) { backStackEntry ->
            val filamentId = backStackEntry.arguments?.getLong("filamentId") ?: return@composable
            FilamentDetailScreen(
                filamentId = filamentId,
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
