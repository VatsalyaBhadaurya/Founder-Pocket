package com.vatsalya.founderpocket.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.vatsalya.founderpocket.ui.capture.CaptureScreen
import com.vatsalya.founderpocket.ui.detail.CaptureDetailScreen
import com.vatsalya.founderpocket.ui.main.MainScreen

object Routes {
    const val MAIN   = "main"
    const val DETAIL = "detail/{captureId}"
    const val EDIT   = "edit/{captureId}"
    // Legacy routes kept for share-intent deep link
    const val CAPTURE = "capture"

    fun detail(id: Long) = "detail/$id"
    fun edit(id: Long)   = "edit/$id"
}

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Routes.MAIN) {

        composable(Routes.MAIN) {
            MainScreen(
                onNavigateToDetail = { id -> navController.navigate(Routes.detail(id)) },
                onNavigateToEdit   = { id -> navController.navigate(Routes.edit(id)) }
            )
        }

        // Share-intent entry point — new capture (no captureId)
        composable(Routes.CAPTURE) {
            CaptureScreen(onSaved = { navController.popBackStack() })
        }

        composable(
            route = Routes.DETAIL,
            arguments = listOf(navArgument("captureId") { type = NavType.LongType })
        ) {
            CaptureDetailScreen(
                onBack = { navController.popBackStack() },
                onEdit = { id -> navController.navigate(Routes.edit(id)) }
            )
        }

        composable(
            route = Routes.EDIT,
            arguments = listOf(navArgument("captureId") { type = NavType.LongType })
        ) {
            CaptureScreen(onSaved = { navController.popBackStack() })
        }
    }
}
