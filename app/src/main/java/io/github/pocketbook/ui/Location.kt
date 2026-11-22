package io.github.pocketbook.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
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

/** Live location while [active] is true, e.g. while guiding you back to a pin. Stops automatically. */
@SuppressLint("MissingPermission")
@Composable
fun rememberLiveLocation(active: Boolean): State<Location?> {
    val context = LocalContext.current
    val state = remember { mutableStateOf<Location?>(null) }
    DisposableEffect(active) {
        val lm = context.getSystemService(LocationManager::class.java)
        val provider = lm?.let { bestProvider(it) }
        val listener = SimpleListener { state.value = it }
        if (active && lm != null && provider != null && hasLocationPermission(context)) {
            lm.requestLocationUpdates(provider, 2000L, 2f, listener, Looper.getMainLooper())
        }
        onDispose { runCatching { lm?.removeUpdates(listener) } }
    }
    return state
}

/** Compass heading in degrees from north, or null if the phone has no suitable sensor. */
@Composable
fun rememberHeading(active: Boolean): State<Float?> {
    val context = LocalContext.current
    val heading = remember { mutableStateOf<Float?>(null) }
    val smooth = remember { mutableFloatStateOf(0f) }
    DisposableEffect(active) {
        val sm = context.getSystemService(SensorManager::class.java)
        val sensor = sm?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val listener = object : SensorEventListener {
            val rot = FloatArray(9); val orient = FloatArray(3)
            override fun onSensorChanged(e: SensorEvent) {
                SensorManager.getRotationMatrixFromVector(rot, e.values)
                SensorManager.getOrientation(rot, orient)
                val deg = ((Math.toDegrees(orient[0].toDouble()) + 360) % 360).toFloat()
                var diff = deg - smooth.floatValue
                if (diff > 180) diff -= 360
                if (diff < -180) diff += 360
                smooth.floatValue = (smooth.floatValue + diff * 0.2f + 360) % 360
                heading.value = smooth.floatValue
            }
            override fun onAccuracyChanged(s: Sensor?, a: Int) {}
        }
        if (active && sensor != null) sm?.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        onDispose { sm?.unregisterListener(listener) }
    }
    return heading
}
