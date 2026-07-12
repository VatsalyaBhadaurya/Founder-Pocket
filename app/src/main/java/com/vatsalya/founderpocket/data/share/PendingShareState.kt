package com.vatsalya.founderpocket.data.share

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Holds shared text from an ACTION_SEND intent until CaptureViewModel consumes it.
 * Spike D: share intent bridge between MainActivity and CaptureViewModel.
 */
@Singleton
class PendingShareState @Inject constructor() {

    data class SharePayload(val text: String, val sourceApp: String? = null)

    private val _payload = MutableStateFlow<SharePayload?>(null)
    val payload: StateFlow<SharePayload?> = _payload.asStateFlow()

    fun post(text: String, sourceApp: String? = null) {
        _payload.value = SharePayload(text, sourceApp)
    }

    /** Read and clear atomically. Returns null if nothing pending. */
    fun consume(): SharePayload? {
        val current = _payload.value ?: return null
        _payload.value = null
        return current
    }
}
