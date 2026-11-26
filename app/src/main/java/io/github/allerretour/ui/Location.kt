package io.github.allerretour.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import androidx.core.content.ContextCompat

/*
 * Location is used only on demand, through the platform LocationManager.
 * No Google Play Services, no background access, nothing leaves the phone.
 */

fun hasLocationPermission(context: Context) =
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

private fun bestProvider(lm: LocationManager): String? =
    listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER).firstOrNull { runCatching { lm.isProviderEnabled(it) }.getOrDefault(false) }

private open class SimpleListener(val onFix: (Location) -> Unit) : LocationListener {
    override fun onLocationChanged(location: Location) = onFix(location)
    @Deprecated("Required on Android 8–10") override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}
}

/** One-off location fix. Calls back with null if location is off or unavailable. */
@SuppressLint("MissingPermission")
fun oneOffLocation(context: Context, onResult: (Location?) -> Unit) {
    if (!hasLocationPermission(context)) return onResult(null)
    val lm = context.getSystemService(LocationManager::class.java) ?: return onResult(null)
    val provider = bestProvider(lm) ?: return onResult(null)
    if (Build.VERSION.SDK_INT >= 30) {
        lm.getCurrentLocation(provider, null, context.mainExecutor) { loc -> onResult(loc ?: lm.getLastKnownLocation(provider)) }
    } else {
        @Suppress("DEPRECATION")
        lm.requestSingleUpdate(provider, SimpleListener { onResult(it) }, Looper.getMainLooper())
    }
}
