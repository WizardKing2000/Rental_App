package com.example.ra

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.SystemClock
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import android.os.CancellationSignal
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal data class LocationControl(
    val busy: Boolean, val message: String?, val needsSettings: Boolean,
    val request: () -> Unit, val cancel: () -> Unit
)

@Composable
internal fun rememberLocationControl(reason: String, onLocated: (GeoPoint) -> Unit): LocationControl {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()
    val latestLocated by rememberUpdatedState(onLocated)
    var job by remember { mutableStateOf<Job?>(null) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var needsSettings by remember { mutableStateOf(false) }
    var rationale by remember { mutableStateOf(false) }
    var permissionPending by remember { mutableStateOf(false) }
    var generation by remember { mutableStateOf(0) }

    fun cancel() {
        generation++
        job?.cancel()
        job = null
        busy = false
        message = null
        needsSettings = false
        permissionPending = false
    }
    fun locate() {
        cancel()
        val ticket = generation
        busy = true
        job = scope.launch {
            try {
                val point = currentPosition(context)
                if (generation == ticket) latestLocated(point)
            } catch (_: TimeoutCancellationException) {
                if (generation == ticket) message = "Couldn't get your location. Check that location is on, then try again."
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: SecurityException) {
                if (generation == ticket) {
                    message = "Location access is off. You can still use the search field."
                    needsSettings = true
                }
            } catch (failure: LocationProblem) {
                if (generation == ticket) message = failure.message
            } catch (_: Exception) {
                if (generation == ticket) message = "Couldn't get your location. Please try again."
            } finally {
                if (generation == ticket) busy = false
            }
        }
    }
    val permissions = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        if (permissionPending) {
            permissionPending = false
            if (grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true) locate()
            else {
                message = "Allow location to use this feature. You can still use the search field."
                needsSettings = activity != null &&
                    !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_COARSE_LOCATION)
            }
        }
    }
    fun launchPermission() {
        permissionPending = true
        permissions.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }
    DisposableEffect(activity) {
        val lifecycle = (activity as? LifecycleOwner)?.lifecycle
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && !permissionPending) cancel()
        }
        lifecycle?.addObserver(observer)
        onDispose { lifecycle?.removeObserver(observer); job?.cancel() }
    }
    if (rationale) AlertDialog(onDismissRequest = { rationale = false },
        title = { Text("Use your location") }, text = { Text(reason) },
        confirmButton = { TextButton(onClick = { rationale = false; launchPermission() }) { Text("Continue") } },
        dismissButton = { TextButton(onClick = { rationale = false }) { Text("Cancel") } })
    return LocationControl(busy, message, needsSettings, request = {
        message = null
        needsSettings = false
        when {
            hasLocation(context) -> locate()
            activity != null && ActivityCompat.shouldShowRequestPermissionRationale(
                activity, Manifest.permission.ACCESS_COARSE_LOCATION) -> rationale = true
            else -> launchPermission()
        }
    }, cancel = { cancel() })
}

internal fun openLocationAppSettings(context: Context) {
    context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", context.packageName, null)).apply {
        if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    })
}

private fun hasLocation(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

private class LocationProblem(message: String) : Exception(message)

/** One foreground fix. The renter's position is never written to preferences or Firebase. */
internal suspend fun currentPosition(context: Context): GeoPoint = withTimeout(25_000) {
    if (!hasLocation(context)) throw SecurityException()
    val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    if (!LocationManagerCompat.isLocationEnabled(manager))
        throw LocationProblem("Location is turned off. Turn on location and try again.")
    val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED
    val providers = (listOf("fused", LocationManager.NETWORK_PROVIDER) +
        if (fine) listOf(LocationManager.GPS_PROVIDER) else emptyList()).filter {
        LocationManagerCompat.hasProvider(manager, it) && manager.isProviderEnabled(it)
    }
    if (providers.isEmpty()) throw LocationProblem("No location service is available. Turn on location and try again.")
    val recent = providers.mapNotNull { manager.getLastKnownLocation(it) }
        .filter { location ->
            val age = (SystemClock.elapsedRealtimeNanos() - location.elapsedRealtimeNanos) / 1_000_000_000.0
            age in 0.0..120.0 && location.accuracy <= 5000 &&
                GeoPoint.from(location.latitude, location.longitude) != null
        }.maxByOrNull { it.elapsedRealtimeNanos }
    if (recent != null) return@withTimeout GeoPoint(recent.latitude, recent.longitude)
    // Start providers together: a slow network fix must not delay a usable GPS fix.
    coroutineScope {
        val fixes = Channel<Location?>(Channel.UNLIMITED)
        val requests = providers.map { provider ->
            launch {
                val fix = try {
                    withTimeoutOrNull(20_000) {
                        suspendCancellableCoroutine<Location?> { continuation ->
                            val signal = CancellationSignal()
                            continuation.invokeOnCancellation { signal.cancel() }
                            try {
                                LocationManagerCompat.getCurrentLocation(manager, provider, signal,
                                    ContextCompat.getMainExecutor(context)) {
                                    if (continuation.isActive) continuation.resume(it)
                                }
                            } catch (failure: Exception) {
                                if (continuation.isActive) continuation.resumeWithException(failure)
                            }
                        }
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) { null }
                fixes.send(fix)
            }
        }
        try {
            repeat(providers.size) {
                val fix = fixes.receive()
                if (fix != null) {
                    val age = (SystemClock.elapsedRealtimeNanos() - fix.elapsedRealtimeNanos) / 1_000_000_000.0
                    val point = GeoPoint.from(fix.latitude, fix.longitude)
                    if (point != null && age in 0.0..120.0 && fix.hasAccuracy() && fix.accuracy <= 5000)
                        return@coroutineScope point
                }
            }
            throw LocationProblem("Couldn't get a current location. Check location accuracy in your phone settings and try again.")
        } finally {
            requests.forEach { it.cancel() }
            fixes.close()
        }
    }
}
