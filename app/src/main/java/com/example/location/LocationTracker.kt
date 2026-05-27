package com.example.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Interface unificada para captura de geolocalização de forma segura e reativa.
 */
interface LocationTracker {
    suspend fun getCurrentLocation(): Location?
}

/**
 * Implementação padrão utilizando FusedLocationProviderClient do Google Play Services.
 */
class DefaultLocationTracker(
    private val context: Context,
    private val locationClient: FusedLocationProviderClient
) : LocationTracker {

    override suspend fun getCurrentLocation(): Location? {
        val hasFineLocationPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val hasCoarseLocationPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        // Verifica se os serviços de GPS ou Redes estão habilitados
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        if (!hasFineLocationPermission && !hasCoarseLocationPermission) {
            return null
        }

        if (!isGpsEnabled) {
            return null
        }

        return suspendCancellableCoroutine { continuation ->
            val cancellationTokenSource = CancellationTokenSource()

            locationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cancellationTokenSource.token
            ).addOnSuccessListener { location ->
                if (continuation.isActive) {
                    continuation.resume(location)
                }
            }.addOnFailureListener {
                if (continuation.isActive) {
                    continuation.resume(null)
                }
            }.addOnCanceledListener {
                if (continuation.isActive) {
                    continuation.resume(null)
                }
            }

            continuation.invokeOnCancellation {
                cancellationTokenSource.cancel()
            }
        }
    }
}
