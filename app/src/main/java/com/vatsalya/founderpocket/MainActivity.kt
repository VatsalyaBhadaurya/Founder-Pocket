package com.vatsalya.founderpocket

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.navigation.compose.rememberNavController
import com.vatsalya.founderpocket.data.share.PendingShareState
import com.vatsalya.founderpocket.ui.navigation.NavGraph
import com.vatsalya.founderpocket.ui.navigation.Routes
import com.vatsalya.founderpocket.ui.theme.FounderPocketTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var pendingShare: PendingShareState

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleShareIntent(intent)
        enableEdgeToEdge()
        setContent {
            FounderPocketTheme {
                val navController = rememberNavController()

                // Spike D: auto-navigate to Capture when a share intent arrives
                val sharePayload by pendingShare.payload.collectAsState()
                LaunchedEffect(sharePayload) {
                    if (sharePayload != null) {
                        navController.navigate(Routes.CAPTURE) { launchSingleTop = true }
                    }
                }

                NavGraph(navController = navController)
            }
        }
    }

    // Called when app is already running and receives a new share intent (singleTop)
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleShareIntent(intent)
    }

    private fun handleShareIntent(intent: Intent?) {
        if (intent?.action != Intent.ACTION_SEND) return
        if (intent.type?.startsWith("text/") != true) return
        val text = intent.getStringExtra(Intent.EXTRA_TEXT)
            ?: intent.getStringExtra(Intent.EXTRA_SUBJECT)
            ?: return
        pendingShare.post(text, callingPackage)
    }
}
