package com.vatsalya.founderpocket.ui.security

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Wraps [content] behind a biometric (or device-credential) authentication gate.
 * When [isLocked] is true the gate shows a lock overlay and immediately triggers
 * the system biometric prompt. If no auth method is enrolled, [onUnlock] is called
 * silently so the user is never permanently locked out.
 */
@Composable
fun BiometricGate(
    isLocked: Boolean,
    onUnlock: () -> Unit,
    content: @Composable () -> Unit
) {
    if (!isLocked) {
        content()
        return
    }

    val activity = LocalContext.current as FragmentActivity
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val canAuthenticate = remember {
        BiometricManager.from(activity).canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL
        ) == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun showPrompt() {
        errorMessage = null
        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onUnlock()
                }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    if (errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                        errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                        errorMessage = errString.toString()
                    }
                }
            }
        )
        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock Founder Pocket")
                .setSubtitle("Confirm your identity to continue")
                .setAllowedAuthenticators(
                    BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
                )
                .build()
        )
    }

    // On first composition show the prompt (or silently unlock if no auth is configured)
    LaunchedEffect(Unit) {
        if (canAuthenticate) showPrompt() else onUnlock()
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text("Founder Pocket is locked", style = MaterialTheme.typography.titleMedium)
            errorMessage?.let { msg ->
                Text(
                    text = msg,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            if (canAuthenticate) {
                Button(onClick = ::showPrompt, modifier = Modifier.fillMaxWidth()) {
                    Text("Unlock")
                }
            }
        }
    }
}
