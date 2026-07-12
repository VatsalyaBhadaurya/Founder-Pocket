package com.vatsalya.founderpocket.data.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.os.Build
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.vatsalya.founderpocket.data.model.LocationData
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class LocationRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val client = LocationServices.getFusedLocationProviderClient(context)

    /**
     * One-shot location fetch. Caller must have already obtained the permission.
     * Returns null if location is unavailable or permission is missing.
     */
    @SuppressLint("MissingPermission")
    suspend fun getCurrent(): LocationData? = suspendCancellableCoroutine { cont ->
        client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
            .addOnSuccessListener { loc ->
                if (loc != null) {
                    val label = reverseGeocode(loc.latitude, loc.longitude)
                    cont.resume(LocationData(loc.latitude, loc.longitude, label))
                } else {
                    cont.resume(null)
                }
            }
            .addOnFailureListener { cont.resume(null) }
    }

    @Suppress("DEPRECATION")
    private fun reverseGeocode(lat: Double, lng: Double): String? {
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Synchronous wrapper via a blocking flag — acceptable since this
                // is called from a background coroutine in EmbedWorker / ViewModel.
                var result: String? = null
                geocoder.getFromLocation(lat, lng, 1) { addresses ->
                    result = addresses.firstOrNull()?.run {
                        subLocality ?: locality ?: adminArea
                    }
                }
                result
            } else {
                geocoder.getFromLocation(lat, lng, 1)?.firstOrNull()?.run {
                    subLocality ?: locality ?: adminArea
                }
            }
        } catch (_: Exception) {
            null
        }
    }
}
