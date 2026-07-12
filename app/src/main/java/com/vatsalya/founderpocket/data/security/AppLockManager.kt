package com.vatsalya.founderpocket.data.security

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tracks app foreground/background cycles and locks the UI when the app has been
 * in the background for more than LOCK_TIMEOUT_MS. The lock is cleared by a successful
 * biometric (or device credential) authentication in BiometricGate.
 *
 * All state is in-memory: lock resets to false on cold start, which is the right default
 * since the device was just unlocked by the OS.
 */
@Singleton
class AppLockManager @Inject constructor() {

    private val _isLocked = MutableStateFlow(false)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    private var lastPauseMillis = 0L

    fun onPause() {
        lastPauseMillis = System.currentTimeMillis()
    }

    fun onResume() {
        val elapsed = System.currentTimeMillis() - lastPauseMillis
        if (lastPauseMillis > 0L && elapsed > LOCK_TIMEOUT_MS) {
            _isLocked.value = true
        }
    }

    fun unlock() {
        _isLocked.value = false
        lastPauseMillis = 0L
    }

    fun lockNow() {
        _isLocked.value = true
    }

    companion object {
        private const val LOCK_TIMEOUT_MS = 60_000L
    }
}
