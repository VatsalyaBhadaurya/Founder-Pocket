package com.vatsalya.founderpocket.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.vatsalya.founderpocket.ui.assistant.AssistantScreen
import com.vatsalya.founderpocket.ui.capture.CaptureScreen
import com.vatsalya.founderpocket.ui.focus.FocusTimerScreen
import com.vatsalya.founderpocket.ui.home.HomeScreen
import com.vatsalya.founderpocket.ui.list.CaptureListScreen

object Routes {
    const val HOME      = "home"
    const val CAPTURE   = "capture"
    const val LIST      = "list"
    const val FOCUS     = "focus"
    const val ASSISTANT = "assistant"
}

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onNewCapture  = { navController.navigate(Routes.CAPTURE) },
                onViewAll     = { navController.navigate(Routes.LIST) },
                onFocusTimer  = { navController.navigate(Routes.FOCUS) },
                onAssistant   = { navController.navigate(Routes.ASSISTANT) }
            )
        }
        composable(Routes.CAPTURE) {
            CaptureScreen(onSaved = { navController.popBackStack() })
        }
        composable(Routes.LIST) {
            CaptureListScreen(onNewCapture = { navController.navigate(Routes.CAPTURE) })
        }
        composable(Routes.FOCUS) {
            FocusTimerScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.ASSISTANT) {
            AssistantScreen(onBack = { navController.popBackStack() })
        }
    }
}
