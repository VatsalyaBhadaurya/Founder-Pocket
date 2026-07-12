package com.vatsalya.founderpocket

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.*
import androidx.navigation.compose.rememberNavController
import com.vatsalya.founderpocket.data.security.AppLockManager
import com.vatsalya.founderpocket.data.share.PendingShareState
import com.vatsalya.founderpocket.data.store.OnboardingStore
import com.vatsalya.founderpocket.ui.navigation.NavGraph
import com.vatsalya.founderpocket.ui.navigation.Routes
import com.vatsalya.founderpocket.ui.onboarding.OnboardingScreen
import com.vatsalya.founderpocket.ui.security.BiometricGate
import com.vatsalya.founderpocket.ui.theme.FounderPocketTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject lateinit var pendingShare: PendingShareState
    @Inject lateinit var appLockManager: AppLockManager
    @Inject lateinit var onboardingStore: OnboardingStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleShareIntent(intent)
        enableEdgeToEdge()
        setContent {
            FounderPocketTheme {
                val scope = rememberCoroutineScope()
                val navController = rememberNavController()
                val isLocked by appLockManager.isLocked.collectAsState()
                // null = DataStore not yet read (one frame), false = first launch, true = seen
                val onboardingDone by onboardingStore.isCompleted.collectAsState(initial = null)

                val sharePayload by pendingShare.payload.collectAsState()
                LaunchedEffect(sharePayload) {
                    if (sharePayload != null && onboardingDone == true) {
                        navController.navigate(Routes.CAPTURE) { launchSingleTop = true }
                    }
                }

                BiometricGate(isLocked = isLocked, onUnlock = appLockManager::unlock) {
                    when (onboardingDone) {
                        null  -> {} // DataStore not loaded yet — blank for one frame
                        false -> OnboardingScreen(
                            onComplete = { scope.launch { onboardingStore.markCompleted() } }
                        )
                        true  -> NavGraph(navController = navController)
                    }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        appLockManager.onPause()
    }

    override fun onResume() {
        super.onResume()
        appLockManager.onResume()
    }

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
